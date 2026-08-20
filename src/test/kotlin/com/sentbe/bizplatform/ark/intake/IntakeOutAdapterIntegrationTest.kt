package com.sentbe.bizplatform.ark.intake

import com.sentbe.bizplatform.ark.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.ark.document.application.service.S3StorageService
import com.sentbe.bizplatform.ark.intake.application.port.out.IntakeOutPort
import com.sentbe.bizplatform.ark.support.ArkTestContainerInitializer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArkTestContainerInitializer::class])
class IntakeOutAdapterIntegrationTest : FunSpec() {
	@MockitoBean
	lateinit var s3StorageService: S3StorageService

	@Autowired
	lateinit var intakeOutPort: IntakeOutPort

	@Autowired
	lateinit var jdbc: JdbcClient

	init {
		context("IntakeOutAdapter") {
			val customerId = UUID.randomUUID()
			lateinit var caseId: UUID

			beforeEach {
				caseId = UUID.randomUUID()
				jdbc
					.sql(
						"INSERT INTO onboarding_case (id, customer_id, status) VALUES (:id, :customerId, 'INQUIRY_RECEIVED')",
					).param("id", caseId)
					.param("customerId", customerId)
					.update()
			}

			afterEach {
				jdbc.sql("DELETE FROM intake_response WHERE case_id = :caseId").param("caseId", caseId).update()
				jdbc.sql("DELETE FROM onboarding_case WHERE id = :id").param("id", caseId).update()
			}

			context("save and findByCaseIdAndPhase") {
				test("answers jsonb 왕복 — 저장한 answers를 그대로 조회") {
					val answers = mapOf<String, Any>("company_name" to "SentBe", "employee_count" to 50)
					val intake = IntakeResponse(caseId = caseId, phase = "first", answers = answers)

					intakeOutPort.save(intake)
					val found = intakeOutPort.findByCaseIdAndPhase(caseId, "first")

					found shouldNotBe null
					found!!.caseId shouldBe caseId
					found.phase shouldBe "first"
					found.answers shouldContainKey "company_name"
					found.answers["company_name"] shouldBe "SentBe"
				}

				test("upsert — 같은 phase를 두 번 저장하면 마지막 값으로 덮어씀") {
					val first = IntakeResponse(caseId = caseId, phase = "first", answers = mapOf("v" to 1))
					val updated = IntakeResponse(caseId = caseId, phase = "first", answers = mapOf("v" to 2))

					intakeOutPort.save(first)
					intakeOutPort.save(updated)
					val found = intakeOutPort.findByCaseIdAndPhase(caseId, "first")

					found!!.answers["v"] shouldBe 2
				}

				test("존재하지 않는 phase는 null 반환") {
					val result = intakeOutPort.findByCaseIdAndPhase(caseId, "second")
					result shouldBe null
				}
			}
		}
	}
}
