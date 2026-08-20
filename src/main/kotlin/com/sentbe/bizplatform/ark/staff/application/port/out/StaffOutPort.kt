package com.sentbe.bizplatform.ark.staff.application.port.out

import com.sentbe.bizplatform.ark.staff.application.domain.Staff
import com.sentbe.bizplatform.ark.staff.application.domain.StaffSession

interface StaffOutPort {
	fun findByEmailAndIsActive(
		email: String,
		isActive: Boolean,
	): Staff?

	fun saveSession(session: StaffSession)
}
