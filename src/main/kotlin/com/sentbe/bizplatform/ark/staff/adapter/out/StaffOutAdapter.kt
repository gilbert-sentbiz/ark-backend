package com.sentbe.bizplatform.ark.staff.adapter.out

import com.sentbe.bizplatform.ark.staff.application.domain.Staff
import com.sentbe.bizplatform.ark.staff.application.domain.StaffSession
import com.sentbe.bizplatform.ark.staff.application.port.out.StaffOutPort
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class StaffOutAdapter(
	private val staffRepo: StaffRepository,
	private val sessionRepo: StaffSessionRepository,
) : StaffOutPort {
	override fun findByEmailAndIsActive(
		email: String,
		isActive: Boolean,
	): Staff? = staffRepo.findByEmailAndIsActive(email, isActive)?.toDomain()

	override fun saveSession(session: StaffSession) {
		sessionRepo.save(session.toEntity())
	}

	private fun StaffJdbcEntity.toDomain() =
		Staff(
			id = id,
			email = email,
			name = name,
			role = role,
			isActive = isActive,
			createdAt = createdAt?.atOffset(ZoneOffset.UTC) ?: OffsetDateTime.now(),
		)

	private fun StaffSession.toEntity() =
		StaffSessionJdbcEntity(
			id = id,
			staffId = staffId,
			token = token,
			expiresAt = expiresAt,
		)
}
