package com.sentbe.bizplatform.arc.health

import com.sentbe.bizplatform.arc.document.application.service.S3StorageService
import com.sentbe.bizplatform.arc.global.auth.CustomerAuthFilter
import com.sentbe.bizplatform.arc.global.auth.StaffAuthFilter
import com.sentbe.bizplatform.arc.support.ArcTestContainerInitializer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ActiveProfiles("local")
@ContextConfiguration(initializers = [ArcTestContainerInitializer::class])
class HealthControllerTest : DescribeSpec() {
    @MockitoBean
    lateinit var s3StorageService: S3StorageService

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var customerAuthFilterReg: FilterRegistrationBean<CustomerAuthFilter>

    @Autowired
    lateinit var staffAuthFilterReg: FilterRegistrationBean<StaffAuthFilter>

    private lateinit var mockMvc: MockMvc

    init {
        beforeSpec {
            val builder = MockMvcBuilders.webAppContextSetup(wac)
            builder.addFilter<DefaultMockMvcBuilder>(customerAuthFilterReg.filter!! as Filter, "/*")
            builder.addFilter<DefaultMockMvcBuilder>(staffAuthFilterReg.filter!! as Filter, "/internal/*")
            mockMvc = builder.build()
        }

        describe("GET /actuator/health") {
            it("200 OK를 반환한다") {
                val result =
                    mockMvc
                        .perform(MockMvcRequestBuilders.get("/actuator/health"))
                        .andExpect(MockMvcResultMatchers.status().isOk)
                        .andReturn()

                result.response.status shouldBe 200
            }
        }
    }
}
