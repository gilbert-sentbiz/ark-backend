package com.sentbe.bizplatform.arc.customer.adapter.out

import com.sentbe.bizplatform.arc.customer.application.domain.CustomerSession
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CustomerSessionRepository : CrudRepository<CustomerSession, UUID> {
	fun findByToken(token: String): CustomerSession?
}
