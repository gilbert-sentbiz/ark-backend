package com.sentbe.bizplatform.arc.staff.adapter.out

import com.sentbe.bizplatform.arc.staff.application.domain.StaffSession
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface StaffSessionRepository : CrudRepository<StaffSession, UUID> {
	fun findByToken(token: String): StaffSession?
}
