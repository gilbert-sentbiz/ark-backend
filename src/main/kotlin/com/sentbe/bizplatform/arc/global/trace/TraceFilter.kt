package com.sentbe.bizplatform.arc.global.trace

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.OffsetDateTime
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceFilter : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		try {
			TraceContext.set(
				traceNo =
					UUID
						.randomUUID()
						.toString()
						.replace("-", "")
						.substring(0, 20),
				path = request.requestURI,
				requestDateTime = OffsetDateTime.now(),
			)
			filterChain.doFilter(request, response)
		} finally {
			TraceContext.clear()
		}
	}
}
