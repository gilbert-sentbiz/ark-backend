package com.sentbe.bizplatform.arc.staff.adapter.out

import com.sentbe.bizplatform.arc.staff.application.domain.Staff
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface StaffRepository : CrudRepository<Staff, UUID> {
	fun findByEmailAndIsActive(
		email: String,
		isActive: Boolean,
	): Staff?
}
