package com.sentbe.bizplatform.ark.rule

import com.sentbe.bizplatform.ark.document.application.service.S3StorageService
import com.sentbe.bizplatform.ark.rule.application.port.out.RuleQueryPort
import com.sentbe.bizplatform.ark.support.ArkTestContainerInitializer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArkTestContainerInitializer::class])
class RuleQueryAdapterIntegrationTest : FunSpec() {
	@MockitoBean
	lateinit var s3StorageService: S3StorageService

	@Autowired
	lateinit var ruleQueryPort: RuleQueryPort

	@Autowired
	lateinit var jdbc: JdbcClient

	init {
		context("RuleQueryAdapter") {
			context("findActiveSegments") {
				test("시드 segment 3건 반환") {
					val segments = ruleQueryPort.findActiveSegments()
					segments.size shouldBe 3
				}

				test("deactivated_at 설정 segment 제외") {
					val id = UUID.randomUUID()
					jdbc
						.sql(
							"INSERT INTO segment (id, axis, code, label, deactivated_at) VALUES (:id, 'entity', 'TEST_DEACT', '삭제 테스트', :at)",
						).param("id", id)
						.param("at", OffsetDateTime.now())
						.update()

					val segments = ruleQueryPort.findActiveSegments()
					segments.none { it.code == "TEST_DEACT" } shouldBe true

					jdbc.sql("DELETE FROM segment WHERE id = :id").param("id", id).update()
				}
			}

			context("findActiveQuestions") {
				test("활성 question 목록 반환") {
					val questions = ruleQueryPort.findActiveQuestions()
					questions.shouldNotBeEmpty()
				}

				test("deactivated_at 설정 question 제외") {
					val id = UUID.randomUUID()
					jdbc
						.sql(
							"INSERT INTO question (id, code, phase, classification, label, input_type, deactivated_at) VALUES (:id, 'q_deact_test', 'first', 'common', '삭제 테스트', 'text', :at)",
						).param("id", id)
						.param("at", OffsetDateTime.now())
						.update()

					val questions = ruleQueryPort.findActiveQuestions()
					questions.none { it.code == "q_deact_test" } shouldBe true

					jdbc.sql("DELETE FROM question WHERE id = :id").param("id", id).update()
				}
			}

			context("findActiveDocTemplates") {
				test("시드 doc_template 9건 반환") {
					val templates = ruleQueryPort.findActiveDocTemplates()
					templates.size shouldBe 9
				}

				test("deactivated_at 설정 doc_template 제외") {
					val id = UUID.randomUUID()
					jdbc
						.sql(
							"INSERT INTO doc_template (id, type, display_name, classification, deactivated_at) VALUES (:id, 'TEST_DEACT_TYPE', '삭제 테스트', 'common', :at)",
						).param("id", id)
						.param("at", OffsetDateTime.now())
						.update()

					val templates = ruleQueryPort.findActiveDocTemplates()
					templates.none { it.type == "TEST_DEACT_TYPE" } shouldBe true

					jdbc.sql("DELETE FROM doc_template WHERE id = :id").param("id", id).update()
				}
			}
		}
	}
}
