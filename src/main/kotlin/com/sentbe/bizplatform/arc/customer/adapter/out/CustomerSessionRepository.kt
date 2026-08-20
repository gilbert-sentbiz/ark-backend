package com.sentbe.bizplatform.arc.customer.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CustomerSessionRepository : CrudRepository<CustomerSessionJdbcEntity, UUID> {
	fun findByToken(token: String): CustomerSessionJdbcEntity?
}
