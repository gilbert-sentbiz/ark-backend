package com.sentbe.bizplatform.arc.global.auth

import com.sentbe.bizplatform.arc.staff.adapter.out.StaffRepository
import com.sentbe.bizplatform.arc.staff.adapter.out.StaffSessionRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.time.OffsetDateTime

class StaffAuthFilter(
    private val sessionRepo: StaffSessionRepository,
    private val staffRepo: StaffRepository,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/internal/") && !path.startsWith("/rules/")
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
                    val staff = staffRepo.findById(session.staffId).orElse(null)
                    if (staff != null && staff.isActive) {
                        AuthContext.staff = AuthenticatedStaff(staff.id, staff.email, staff.role)
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
