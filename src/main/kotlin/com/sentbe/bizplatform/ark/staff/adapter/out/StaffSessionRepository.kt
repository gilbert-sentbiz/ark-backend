package com.sentbe.bizplatform.ark.staff.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface StaffSessionRepository : CrudRepository<StaffSessionJdbcEntity, UUID> {
	fun findByToken(token: String): StaffSessionJdbcEntity?
}
