package com.sentbe.bizplatform.ark.support

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.sentbe.bizplatform.ark.global.exception.ArkExceptionAdvice
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

object StandaloneMockMvcFactory {
	val objectMapper: ObjectMapper =
		ObjectMapper()
			.findAndRegisterModules()
			.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true)

	fun create(vararg controllers: Any): MockMvc =
		MockMvcBuilders
			.standaloneSetup(*controllers)
			.setControllerAdvice(ArkExceptionAdvice())
			.setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
			.build()
}
