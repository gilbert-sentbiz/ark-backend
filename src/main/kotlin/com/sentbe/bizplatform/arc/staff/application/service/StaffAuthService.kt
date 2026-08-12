package com.sentbe.bizplatform.arc.staff.application.service

import com.sentbe.bizplatform.arc.staff.adapter.out.StaffRepository
import com.sentbe.bizplatform.arc.staff.adapter.out.StaffSessionRepository
import com.sentbe.bizplatform.arc.staff.application.domain.StaffSession
import com.sentbe.bizplatform.arc.staff.application.port.input.StaffAuthUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class StaffAuthService(
    private val staffRepo: StaffRepository,
    private val sessionRepo: StaffSessionRepository,
    @Value("\${arc.auth.staff-session-hours:8}") private val sessionHours: Long,
) : StaffAuthUseCase {
    @Transactional
    override fun mockLogin(email: String): String {
        val staff =
            staffRepo.findByEmailAndIsActive(email, true)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "활성 직원 계정 없음: $email")

        val token = UUID.randomUUID().toString()
        sessionRepo.save(
            StaffSession(
                staffId = staff.id,
                token = token,
                expiresAt = OffsetDateTime.now().plusHours(sessionHours),
            ),
        )
        return token
    }
}
