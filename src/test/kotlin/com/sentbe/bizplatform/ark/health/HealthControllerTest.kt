package com.sentbe.bizplatform.ark.health

import com.sentbe.bizplatform.ark.global.health.HealthController
import com.sentbe.bizplatform.ark.support.StandaloneMockMvcFactory
import io.kotest.core.spec.style.FunSpec
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class HealthControllerTest :
	FunSpec({
		val mockMvc = StandaloneMockMvcFactory.create(HealthController())

		context("GET /health") {
			test("200 OK with snake_case result_code and data.status") {
				mockMvc
					.perform(get("/health"))
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.result_code").value("SUCCESS"))
					.andExpect(jsonPath("$.data.status").value("ok"))
			}
		}
	})
