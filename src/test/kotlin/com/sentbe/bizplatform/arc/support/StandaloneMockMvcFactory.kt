package com.sentbe.bizplatform.arc.support

import com.sentbe.bizplatform.arc.global.exception.ArcExceptionAdvice
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

object StandaloneMockMvcFactory {
	fun create(vararg controllers: Any): MockMvc =
		MockMvcBuilders
			.standaloneSetup(*controllers)
			.setControllerAdvice(ArcExceptionAdvice())
			.build()
}
