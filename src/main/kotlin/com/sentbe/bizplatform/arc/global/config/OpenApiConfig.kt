package com.sentbe.bizplatform.arc.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
	@Bean
	fun openApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("ARC Onboarding API")
					.description("SentBe B2B 고객 온보딩 플랫폼 API")
					.version("v1"),
			)
}
