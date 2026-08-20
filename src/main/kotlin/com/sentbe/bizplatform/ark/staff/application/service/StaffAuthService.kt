package com.sentbe.bizplatform.ark.staff.application.service

import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import com.sentbe.bizplatform.ark.staff.application.domain.StaffSession
import com.sentbe.bizplatform.ark.staff.application.port.`in`.StaffAuthPort
import com.sentbe.bizplatform.ark.staff.application.port.out.StaffOutPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class StaffAuthService(
	private val outPort: StaffOutPort,
	@Value("\${ark.auth.staff-session-hours:8}") private val sessionHours: Long,
) : StaffAuthPort {
	@Transactional
	override fun mockLogin(email: String): String {
		val staff =
			outPort.findByEmailAndIsActive(email, true)
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)

		val token = UUID.randomUUID().toString()
		outPort.saveSession(
			StaffSession(
				staffId = staff.id,
				token = token,
				expiresAt = OffsetDateTime.now().plusHours(sessionHours),
			),
		)
		return token
	}
}
