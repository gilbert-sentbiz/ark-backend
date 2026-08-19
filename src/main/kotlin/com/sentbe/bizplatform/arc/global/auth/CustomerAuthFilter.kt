package com.sentbe.bizplatform.arc.global.auth

import com.sentbe.bizplatform.arc.customer.adapter.out.CustomerRepository
import com.sentbe.bizplatform.arc.customer.adapter.out.CustomerSessionRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.time.OffsetDateTime

class CustomerAuthFilter(
	private val sessionRepo: CustomerSessionRepository,
	private val customerRepo: CustomerRepository,
) : OncePerRequestFilter() {
	override fun shouldNotFilter(request: HttpServletRequest): Boolean {
		val path = request.requestURI
		return path.startsWith("/internal/") ||
			path.startsWith("/auth/") ||
			path.startsWith("/actuator/") ||
			path == "/health"
	}

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		chain: FilterChain,
	) {
		try {
			val token = extractBearer(request)
			if (token != null) {
				val session = sessionRepo.findByToken(token)
				if (session != null && session.expiresAt.isAfter(OffsetDateTime.now())) {
					val customer = customerRepo.findById(session.customerId).orElse(null)
					if (customer != null) {
						if (customer.id != null) {
							AuthContext.customer = AuthenticatedCustomer(customer.id, customer.email)
						}
					}
				}
			}
			chain.doFilter(request, response)
		} finally {
			AuthContext.clear()
		}
	}

	private fun extractBearer(request: HttpServletRequest): String? {
		val header = request.getHeader("Authorization") ?: return null
		if (!header.startsWith("Bearer ")) return null
		return header.removePrefix("Bearer ").trim().takeIf { it.isNotBlank() }
	}
}
