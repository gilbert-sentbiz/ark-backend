package com.sentbe.bizplatform.ark.customer

import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.ark.customer.application.port.out.CustomerOutPort
import com.sentbe.bizplatform.ark.document.application.service.S3StorageService
import com.sentbe.bizplatform.ark.support.ArkTestContainerInitializer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
class CustomerOutAdapterIntegrationTest : FunSpec() {
	@MockitoBean
	lateinit var s3StorageService: S3StorageService

	@Autowired
	lateinit var customerOutPort: CustomerOutPort

	@Autowired
	lateinit var jdbc: JdbcClient

	init {
		context("CustomerOutAdapter") {
			val testEmail = "test-${UUID.randomUUID()}@example.com"
			lateinit var savedCustomerId: UUID

			afterEach {
				jdbc.sql("DELETE FROM customer_session WHERE customer_id = :id").param("id", savedCustomerId).update()
				jdbc.sql("DELETE FROM customer WHERE email = :email").param("email", testEmail).update()
			}

			context("saveCustomer") {
				test("저장 후 생성된 ID 반환") {
					val customer = Customer(email = testEmail, authMethod = "otp")
					val saved = customerOutPort.saveCustomer(customer)
					savedCustomerId = saved.id!!

					saved.id shouldNotBe null
					saved.email shouldBe testEmail
				}
			}

			context("findByEmail") {
				test("저장된 customer 이메일로 조회") {
					val customer = Customer(email = testEmail, authMethod = "otp", companyName = "SentBe")
					val saved = customerOutPort.saveCustomer(customer)
					savedCustomerId = saved.id!!

					val found = customerOutPort.findByEmail(testEmail)
					found shouldNotBe null
					found!!.email shouldBe testEmail
					found.companyName shouldBe "SentBe"
				}

				test("존재하지 않는 이메일은 null 반환") {
					savedCustomerId = UUID.randomUUID()
					val found = customerOutPort.findByEmail("notexist-${UUID.randomUUID()}@example.com")
					found shouldBe null
				}
			}

			context("saveSession") {
				test("customer_session 저장") {
					val customer = Customer(email = testEmail, authMethod = "otp")
					val saved = customerOutPort.saveCustomer(customer)
					savedCustomerId = saved.id!!

					val session =
						CustomerSession(
							customerId = savedCustomerId,
							token = "tok-${UUID.randomUUID()}",
							expiresAt = OffsetDateTime.now().plusHours(1),
						)
					customerOutPort.saveSession(session)

					val count =
						jdbc
							.sql("SELECT COUNT(*) FROM customer_session WHERE customer_id = :id")
							.param("id", savedCustomerId)
							.query(Int::class.java)
							.single()
					count shouldBe 1
				}
			}
		}
	}
}
