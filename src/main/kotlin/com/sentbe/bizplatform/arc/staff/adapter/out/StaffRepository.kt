package com.sentbe.bizplatform.arc.staff.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface StaffRepository : CrudRepository<StaffJdbcEntity, UUID> {
	fun findByEmailAndIsActive(
		email: String,
		isActive: Boolean,
	): StaffJdbcEntity?
}
