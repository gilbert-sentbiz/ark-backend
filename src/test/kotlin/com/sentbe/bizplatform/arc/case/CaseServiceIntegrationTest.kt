package com.sentbe.bizplatform.arc.case

import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.case.application.port.input.CaseUseCase
import com.sentbe.bizplatform.arc.document.application.service.S3StorageService
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.arc.support.ArcTestContainerInitializer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArcTestContainerInitializer::class])
class CaseServiceIntegrationTest : DescribeSpec() {
    @MockitoBean
    lateinit var s3StorageService: S3StorageService

    @Autowired
    lateinit var caseUseCase: CaseUseCase

    @Autowired
    lateinit var jdbc: JdbcClient

    private val salesStaff =
        AuthenticatedStaff(
            id = UUID.fromString("00000001-0001-0000-0000-000000000000"),
            email = "sales@sentbe.com",
            role = "SALES",
        )
    private val opsStaff =
        AuthenticatedStaff(
            id = UUID.fromString("00000002-0001-0000-0000-000000000000"),
            email = "ops@sentbe.com",
            role = "OPS",
        )
    private val complianceStaff =
        AuthenticatedStaff(
            id = UUID.fromString("00000003-0001-0000-0000-000000000000"),
            email = "compliance@sentbe.com",
            role = "COMPLIANCE",
        )

    init {
        beforeEach { cleanup() }

        describe("케이스 생성") {
            it("1차 질문 ID를 pinnedQuestionIds[first]에 고정한다") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)

                val pinned = case.pinnedQuestionIds["first"] as? List<*>
                pinned shouldNotBe null
                pinned!!.size shouldBeGreaterThan 0
                case.status shouldBe CaseStatus.INQUIRY_RECEIVED
            }

            it("진행 중인 케이스가 있으면 CONFLICT를 던진다") {
                val customerId = insertCustomer()
                caseUseCase.createCase(customerId)

                val ex =
                    shouldThrow<ResponseStatusException> {
                        caseUseCase.createCase(customerId)
                    }
                ex.statusCode shouldBe HttpStatus.CONFLICT
            }
        }

        describe("소급 차단 — 룰 변경 후 기존 케이스 pinnedQuestionIds 불변") {
            it("질문 비활성화 후 기존 케이스의 고정 ID는 그대로 유지된다") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)

                val pinned = case.pinnedQuestionIds["first"] as List<*>
                pinned.size shouldBeGreaterThan 0
                val targetId = pinned.first() as String

                // 질문 비활성화 — getActiveRules()에서 제외됨
                jdbc
                    .sql("UPDATE question SET deactivated_at = now() WHERE id = :id::uuid")
                    .param("id", targetId)
                    .update()

                // 케이스 재조회 — pinnedQuestionIds는 변경 불가 (소급 차단)
                val reloaded = caseUseCase.getCase(case.id)
                val reloadedPinned = reloaded.pinnedQuestionIds["first"] as List<*>
                reloadedPinned shouldContain targetId

                // 롤백: 다른 테스트에 영향 없도록 비활성화 해제
                jdbc
                    .sql("UPDATE question SET deactivated_at = null WHERE id = :id::uuid")
                    .param("id", targetId)
                    .update()
            }
        }

        describe("1차 인테이크 제출 — 중복 제출 불변식") {
            it("draft 임시저장 후 1차 제출에 성공한다 (PI-139)") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)
                val answers =
                    mapOf(
                        "businessType" to "corporation",
                        "foundingCountry" to "KR",
                        "services" to listOf("remittance"),
                    )

                caseUseCase.saveIntake(case.id, customerId, "first", answers)

                val updated = caseUseCase.submitFirstIntake(case.id, customerId, answers)
                val secondPinned = updated.pinnedQuestionIds["second"] as? List<*>
                secondPinned shouldNotBe null
                secondPinned!!.size shouldBeGreaterThan 0
            }

            it("이미 제출된 상태에서 재제출하면 CONFLICT를 던진다") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)
                val answers =
                    mapOf(
                        "businessType" to "corporation",
                        "foundingCountry" to "KR",
                        "services" to listOf("remittance"),
                    )

                caseUseCase.submitFirstIntake(case.id, customerId, answers)

                val ex =
                    shouldThrow<ResponseStatusException> {
                        caseUseCase.submitFirstIntake(case.id, customerId, answers)
                    }
                ex.statusCode shouldBe HttpStatus.CONFLICT
            }

            it("1차 제출 후 2차 질문 ID가 pinnedQuestionIds[second]에 고정된다") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)
                val answers =
                    mapOf(
                        "businessType" to "corporation",
                        "foundingCountry" to "KR",
                        "services" to listOf("remittance"),
                    )

                val updated = caseUseCase.submitFirstIntake(case.id, customerId, answers)
                val secondPinned = updated.pinnedQuestionIds["second"] as? List<*>
                secondPinned shouldNotBe null
                secondPinned!!.size shouldBeGreaterThan 0
            }
        }

        describe("상태 전이 — advanceStatus") {
            it("SALES가 INITIAL_SCREENING → DOCUMENT_SCREENING_REQUIRED로 전진시킨다") {
                val caseId = setupCaseWithStatus(CaseStatus.INITIAL_SCREENING)
                val advanced = caseUseCase.advanceStatus(caseId, salesStaff)
                advanced.status shouldBe CaseStatus.DOCUMENT_SCREENING_REQUIRED
            }

            it("4단계 순차 전진이 COMPLETED로 이어진다") {
                val caseId = setupCaseWithStatus(CaseStatus.INITIAL_SCREENING)
                caseUseCase.advanceStatus(caseId, salesStaff)
                caseUseCase.advanceStatus(caseId, opsStaff)
                caseUseCase.advanceStatus(caseId, complianceStaff)
                val final = caseUseCase.advanceStatus(caseId, opsStaff)
                final.status shouldBe CaseStatus.COMPLETED
            }

            it("역할이 맞지 않으면 FORBIDDEN을 던진다") {
                val caseId = setupCaseWithStatus(CaseStatus.INITIAL_SCREENING)
                val ex =
                    shouldThrow<ResponseStatusException> {
                        caseUseCase.advanceStatus(caseId, opsStaff) // SALES 역할 필요
                    }
                ex.statusCode shouldBe HttpStatus.FORBIDDEN
            }

            it("전진 불가 상태에서는 BAD_REQUEST를 던진다") {
                val caseId = setupCaseWithStatus(CaseStatus.INQUIRY_RECEIVED)
                val ex =
                    shouldThrow<ResponseStatusException> {
                        caseUseCase.advanceStatus(caseId, salesStaff)
                    }
                ex.statusCode shouldBe HttpStatus.BAD_REQUEST
            }
        }

        describe("케이스 이벤트 — append-only 불변식") {
            it("케이스 생성 시 CASE_CREATED 이벤트 1건이 기록된다") {
                val customerId = insertCustomer()
                val case = caseUseCase.createCase(customerId)

                val events = caseUseCase.getCaseTimeline(case.id)
                events.size shouldBe 1
                events.first()["eventType"] shouldBe "CASE_CREATED"
            }

            it("상태 전이 시 이벤트가 누적되고 기존 이벤트는 삭제되지 않는다") {
                val caseId = setupCaseWithStatus(CaseStatus.INITIAL_SCREENING)
                val before = caseUseCase.getCaseTimeline(caseId).size

                caseUseCase.advanceStatus(caseId, salesStaff)

                val after = caseUseCase.getCaseTimeline(caseId)
                after.size shouldBe before + 1
                after.last()["eventType"] shouldBe "CASE_STATUS_CHANGED"
            }
        }

        describe("케이스 재제출 — resubmit") {
            it("미해결 보완 요청이 없으면 REVISION_REQUESTED에서 원래 상태로 복귀한다") {
                val customerId = insertCustomer()
                val caseId =
                    insertCaseWithStatus(
                        customerId = customerId,
                        status = CaseStatus.REVISION_REQUESTED,
                        revisionRequestedFrom = CaseStatus.INITIAL_SCREENING,
                    )

                val result = caseUseCase.resubmit(caseId, customerId)
                result.status shouldBe CaseStatus.INITIAL_SCREENING
            }

            it("REVISION_REQUESTED 상태가 아니면 BAD_REQUEST를 던진다") {
                val customerId = insertCustomer()
                val caseId = insertCaseWithStatus(customerId, CaseStatus.INITIAL_SCREENING)

                val ex =
                    shouldThrow<ResponseStatusException> {
                        caseUseCase.resubmit(caseId, customerId)
                    }
                ex.statusCode shouldBe HttpStatus.BAD_REQUEST
            }
        }
    }

    private fun cleanup() {
        jdbc.sql("DELETE FROM case_event").update()
        jdbc.sql("DELETE FROM intake_response").update()
        jdbc.sql("DELETE FROM revision_request").update()
        jdbc.sql("DELETE FROM document_file").update()
        jdbc.sql("DELETE FROM document").update()
        jdbc.sql("DELETE FROM customer_session").update()
        jdbc.sql("DELETE FROM staff_session").update()
        jdbc.sql("DELETE FROM otp_token").update()
        jdbc.sql("DELETE FROM onboarding_case").update()
        jdbc.sql("DELETE FROM customer").update()
    }

    private fun insertCustomer(email: String = "test@test.com"): UUID {
        val id = UUID.randomUUID()
        jdbc
            .sql("INSERT INTO customer (id, email) VALUES (:id, :email)")
            .param("id", id)
            .param("email", email)
            .update()
        return id
    }

    private fun setupCaseWithStatus(status: String): UUID {
        val customerId = insertCustomer("setup-${UUID.randomUUID()}@test.com")
        return insertCaseWithStatus(customerId, status)
    }

    private fun insertCaseWithStatus(
        customerId: UUID,
        status: String,
        revisionRequestedFrom: String? = null,
    ): UUID {
        val caseId = UUID.randomUUID()
        jdbc
            .sql(
                """INSERT INTO onboarding_case
               (id, customer_id, status, revision_requested_from, services, sectors, segment_meta, pinned_question_ids)
               VALUES (:id, :custId, :status, :revFrom,
                       ARRAY[]::text[], ARRAY[]::text[], '{}'::jsonb, '{}'::jsonb)""",
            ).param("id", caseId)
            .param("custId", customerId)
            .param("status", status)
            .param("revFrom", revisionRequestedFrom)
            .update()
        return caseId
    }
}
