package com.sentbe.bizplatform.ark.support

import com.sentbe.bizplatform.ark.global.exception.ArkExceptionAdvice
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

object StandaloneMockMvcFactory {
	fun create(vararg controllers: Any): MockMvc =
		MockMvcBuilders
			.standaloneSetup(*controllers)
			.setControllerAdvice(ArkExceptionAdvice())
			.build()
}
