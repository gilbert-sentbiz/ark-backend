package com.sentbe.bizplatform.arc.api

import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.document.application.service.S3StorageService
import com.sentbe.bizplatform.arc.global.auth.CustomerAuthFilter
import com.sentbe.bizplatform.arc.global.auth.StaffAuthFilter
import com.sentbe.bizplatform.arc.support.ArcTestContainerInitializer
import jakarta.servlet.Filter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArcTestContainerInitializer::class])
class ArcApiRestDocsTest : DescribeSpec() {
    @MockitoBean
    lateinit var s3StorageService: S3StorageService

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var jdbc: JdbcClient

    @Autowired
    lateinit var customerAuthFilterReg: FilterRegistrationBean<CustomerAuthFilter>

    @Autowired
    lateinit var staffAuthFilterReg: FilterRegistrationBean<StaffAuthFilter>

    private val CUST_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
    private val CUST_TOKEN = "test-customer-restdocs-token"
    private val SALES_ID = UUID.fromString("00000001-0001-0000-0000-000000000000")
    private val SALES_TOKEN = "test-sales-restdocs-token"

    private val restDocumentation = ManualRestDocumentation()
    private lateinit var docsMvc: MockMvc

    init {
        beforeSpec {
            restDocumentation.beforeTest(javaClass, "arc-api-docs")
            val builder = MockMvcBuilders.webAppContextSetup(wac)
            builder.addFilter<DefaultMockMvcBuilder>(customerAuthFilterReg.filter!! as Filter, "/*")
            builder.addFilter<DefaultMockMvcBuilder>(staffAuthFilterReg.filter!! as Filter, "/internal/*")
            docsMvc = builder
                .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
                .build()
        }

        beforeEach {
            cleanupTestData()
            insertCustomerSession()
            insertStaffSession()
        }

        afterSpec {
            restDocumentation.afterTest()
        }

        describe("GET /actuator/health") {
            it("200 OK를 반환한다") {
                val result = docsMvc
                    .perform(MockMvcRequestBuilders.get("/actuator/health"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andDo(MockMvcRestDocumentation.document("health-get"))
                    .andReturn()

                result.response.status shouldBe 200
            }
        }

        describe("GET /rules/active") {
            it("인증된 고객에게 활성 규칙을 반환한다") {
                docsMvc
                    .perform(
                        MockMvcRequestBuilders.get("/rules/active")
                            .header("Authorization", "Bearer $CUST_TOKEN"),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andDo(MockMvcRestDocumentation.document("rules-get-active"))
            }

            it("인증 없이 요청하면 401을 반환한다") {
                docsMvc
                    .perform(MockMvcRequestBuilders.get("/rules/active"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                    .andDo(MockMvcRestDocumentation.document("rules-get-active-unauthorized"))
            }
        }

        describe("POST /cases") {
            it("고객 인증으로 케이스를 생성하고 201을 반환한다") {
                docsMvc
                    .perform(
                        MockMvcRequestBuilders.post("/cases")
                            .header("Authorization", "Bearer $CUST_TOKEN"),
                    ).andExpect(MockMvcResultMatchers.status().isCreated)
                    .andDo(MockMvcRestDocumentation.document("cases-create"))
            }
        }

        describe("GET /internal/cases") {
            it("직원 인증으로 전체 케이스 목록을 반환한다") {
                docsMvc
                    .perform(
                        MockMvcRequestBuilders.get("/internal/cases")
                            .header("Authorization", "Bearer $SALES_TOKEN"),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andDo(MockMvcRestDocumentation.document("internal-cases-list"))
            }
        }

        describe("POST /internal/cases/{id}/advance") {
            it("INITIAL_SCREENING 케이스를 SALES가 전진시키고 200을 반환한다") {
                val caseId = insertCaseForSales()
                docsMvc
                    .perform(
                        MockMvcRequestBuilders.post("/internal/cases/$caseId/advance")
                            .header("Authorization", "Bearer $SALES_TOKEN"),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andDo(MockMvcRestDocumentation.document("internal-case-advance"))
            }
        }
    }

    private fun cleanupTestData() {
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

    private fun insertCustomerSession() {
        jdbc.sql("INSERT INTO customer (id, email) VALUES (:id, :email)")
            .param("id", CUST_ID).param("email", "restdocs@test.com").update()
        jdbc.sql(
            "INSERT INTO customer_session (customer_id, token, expires_at) VALUES (:custId, :token, now() + interval '72 hours')",
        ).param("custId", CUST_ID).param("token", CUST_TOKEN).update()
    }

    private fun insertStaffSession() {
        jdbc.sql(
            "INSERT INTO staff_session (staff_id, token, expires_at) VALUES (:staffId, :token, now() + interval '8 hours')",
        ).param("staffId", SALES_ID).param("token", SALES_TOKEN).update()
    }

    private fun insertCaseForSales(): UUID {
        val caseId = UUID.randomUUID()
        jdbc.sql(
            """INSERT INTO onboarding_case
               (id, customer_id, status, services, sectors, segment_meta, pinned_question_ids)
               VALUES (:id, :custId, :status,
                       ARRAY[]::text[], ARRAY[]::text[], '{}'::jsonb, '{}'::jsonb)""",
        ).param("id", caseId)
            .param("custId", CUST_ID)
            .param("status", CaseStatus.INITIAL_SCREENING)
            .update()
        return caseId
    }
}
