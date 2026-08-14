package com.sentbe.bizplatform.arc.document

import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.document.application.port.input.DocumentUseCase
import com.sentbe.bizplatform.arc.document.application.service.S3StorageService
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.arc.support.ArcTestContainerInitializer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArcTestContainerInitializer::class])
class DocumentServiceIntegrationTest : DescribeSpec() {
    @MockitoBean
    lateinit var s3StorageService: S3StorageService

    @Autowired
    lateinit var documentUseCase: DocumentUseCase

    @Autowired
    lateinit var jdbc: JdbcClient

    private val docTemplateId = UUID.fromString("e0000001-0001-0000-0000-000000000000")
    private val opsStaff =
        AuthenticatedStaff(
            id = UUID.fromString("00000002-0001-0000-0000-000000000000"),
            email = "ops@sentbe.com",
            role = "OPS",
        )

    init {
        beforeEach { cleanup() }

        describe("파일 업로드 — MVP 1파일 제한 불변식 (PI-149)") {
            it("REQUESTED 상태의 서류는 업로드에 성공하고 SUBMITTED로 변경된다") {
                val (customerId, documentId) = setupDocument("REQUESTED")
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(100))

                val detail = documentUseCase.uploadFile(documentId, file, customer)
                detail.document.status shouldBe "SUBMITTED"
            }

            it("이미 최신 파일이 있으면 CONFLICT를 던진다 (PI-149 핵심 불변식)") {
                val (customerId, documentId) = setupDocument("REQUESTED")
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(100))

                documentUseCase.uploadFile(documentId, file, customer)

                val ex =
                    shouldThrow<ResponseStatusException> {
                        documentUseCase.uploadFile(documentId, file, customer)
                    }
                ex.statusCode shouldBe HttpStatus.CONFLICT
            }

            it("SUBMITTED 상태의 서류는 CONFLICT를 던진다") {
                val (customerId, documentId) = setupDocument("SUBMITTED")
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(100))

                val ex =
                    shouldThrow<ResponseStatusException> {
                        documentUseCase.uploadFile(documentId, file, customer)
                    }
                ex.statusCode shouldBe HttpStatus.CONFLICT
            }
        }

        describe("파일 형식 검증") {
            it("허용되지 않는 MIME 타입이면 BAD_REQUEST를 던진다") {
                val (customerId, documentId) = setupDocument("REQUESTED")
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.exe", "application/octet-stream", ByteArray(100))

                val ex =
                    shouldThrow<ResponseStatusException> {
                        documentUseCase.uploadFile(documentId, file, customer)
                    }
                ex.statusCode shouldBe HttpStatus.BAD_REQUEST
            }
        }

        describe("보완 요청 플로우 (PI-147)") {
            it("직원이 SUBMITTED 서류를 반려하면 REVISION_REQUIRED로 변경된다") {
                val (_, documentId) = setupDocument("SUBMITTED")
                val detail = documentUseCase.requestRevision(documentId, opsStaff, "서류 내용 불충분")
                detail.document.status shouldBe "REVISION_REQUIRED"
            }

            it("REVISION_REQUIRED 서류에 파일 업로드 시 SUBMITTED로 전환된다") {
                val (customerId, documentId) = setupDocument("REVISION_REQUIRED", withRevision = true)
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(100))

                val detail = documentUseCase.uploadFile(documentId, file, customer)
                detail.document.status shouldBe "SUBMITTED"
            }

            it("업로드된 파일이 있는 REVISION_REQUIRED 서류도 재업로드에 성공한다 (PI-143)") {
                val (customerId, documentId) = setupDocument("REQUESTED")
                val customer = AuthenticatedCustomer(customerId, "cust@test.com")
                val file = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(100))

                documentUseCase.uploadFile(documentId, file, customer)
                documentUseCase.requestRevision(documentId, opsStaff, "내용 보완 필요")

                val resubmit = documentUseCase.uploadFile(documentId, file, customer)
                resubmit.document.status shouldBe "SUBMITTED"
            }

            it("사유 없이 반려하면 BAD_REQUEST를 던진다") {
                val (_, documentId) = setupDocument("SUBMITTED")
                val ex =
                    shouldThrow<ResponseStatusException> {
                        documentUseCase.requestRevision(documentId, opsStaff, "  ")
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

    private fun setupDocument(
        docStatus: String,
        withRevision: Boolean = false,
    ): Pair<UUID, UUID> {
        val customerId = UUID.randomUUID()
        jdbc
            .sql("INSERT INTO customer (id, email) VALUES (:id, :email)")
            .param("id", customerId)
            .param("email", "cust@test.com")
            .update()

        val caseId = UUID.randomUUID()
        jdbc
            .sql(
                """INSERT INTO onboarding_case
               (id, customer_id, status, services, sectors, segment_meta, pinned_question_ids)
               VALUES (:id, :custId, :caseStatus,
                       ARRAY[]::text[], ARRAY[]::text[], '{}'::jsonb, '{}'::jsonb)""",
            ).param("id", caseId)
            .param("custId", customerId)
            .param("caseStatus", CaseStatus.INITIAL_SCREENING)
            .update()

        val documentId = UUID.randomUUID()
        jdbc
            .sql(
                """INSERT INTO document
               (id, case_id, doc_template_id, type, display_name, status, is_required, is_conditional)
               VALUES (:id, :caseId, :templateId, 'BIZ_REGISTRATION', '사업자등록증', :status, true, false)""",
            ).param("id", documentId)
            .param("caseId", caseId)
            .param("templateId", docTemplateId)
            .param("status", docStatus)
            .update()

        if (withRevision) {
            jdbc
                .sql(
                    """INSERT INTO revision_request
                   (id, document_id, reason, requested_by_staff_id, requested_from_status)
                   VALUES (:id, :docId, '테스트 반려', :staffId, 'INITIAL_SCREENING')""",
                ).param("id", UUID.randomUUID())
                .param("docId", documentId)
                .param("staffId", opsStaff.id)
                .update()
        }

        return customerId to documentId
    }
}
