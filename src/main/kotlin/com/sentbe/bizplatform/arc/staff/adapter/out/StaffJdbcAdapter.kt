package com.sentbe.bizplatform.arc.staff.adapter.out

import com.sentbe.bizplatform.arc.staff.application.domain.Staff
import com.sentbe.bizplatform.arc.staff.application.domain.StaffSession
import com.sentbe.bizplatform.arc.staff.application.port.out.StaffOutPort
import org.springframework.stereotype.Component

@Component
class StaffJdbcAdapter(
	private val staffRepo: StaffRepository,
	private val sessionRepo: StaffSessionRepository,
) : StaffOutPort {
	override fun findByEmailAndIsActive(
		email: String,
		isActive: Boolean,
	): Staff? = staffRepo.findByEmailAndIsActive(email, isActive)

	override fun saveSession(session: StaffSession) {
		sessionRepo.save(session)
	}
}
