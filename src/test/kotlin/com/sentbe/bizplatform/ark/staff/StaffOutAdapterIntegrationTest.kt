package com.sentbe.bizplatform.ark.staff

import com.sentbe.bizplatform.ark.document.application.service.S3StorageService
import com.sentbe.bizplatform.ark.staff.application.domain.StaffSession
import com.sentbe.bizplatform.ark.staff.application.port.out.StaffOutPort
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
class StaffOutAdapterIntegrationTest : FunSpec() {
	@MockitoBean
	lateinit var s3StorageService: S3StorageService

	@Autowired
	lateinit var staffOutPort: StaffOutPort

	@Autowired
	lateinit var jdbc: JdbcClient

	// seed staff ID from V003
	private val seedStaffId = UUID.fromString("00000001-0001-0000-0000-000000000000")
	private val seedStaffEmail = "sales@sentbe.com"

	init {
		context("StaffOutAdapter") {
			context("findByEmailAndIsActive") {
				test("활성 staff 이메일로 조회") {
					val staff = staffOutPort.findByEmailAndIsActive(seedStaffEmail, true)
					staff shouldNotBe null
					staff!!.email shouldBe seedStaffEmail
					staff.role shouldBe "SALES"
					staff.isActive shouldBe true
				}

				test("비활성 조회 조건으로는 null 반환") {
					val staff = staffOutPort.findByEmailAndIsActive(seedStaffEmail, false)
					staff shouldBe null
				}

				test("존재하지 않는 이메일은 null 반환") {
					val staff = staffOutPort.findByEmailAndIsActive("notexist@sentbe.com", true)
					staff shouldBe null
				}
			}

			context("saveSession") {
				test("staff_session 저장") {
					val session =
						StaffSession(
							staffId = seedStaffId,
							token = "stok-${UUID.randomUUID()}",
							expiresAt = OffsetDateTime.now().plusHours(8),
						)
					staffOutPort.saveSession(session)

					val count =
						jdbc
							.sql("SELECT COUNT(*) FROM staff_session WHERE staff_id = :id")
							.param("id", seedStaffId)
							.query(Int::class.java)
							.single()
					count shouldBe 1

					jdbc.sql("DELETE FROM staff_session WHERE staff_id = :id").param("id", seedStaffId).update()
				}
			}
		}
	}
}
