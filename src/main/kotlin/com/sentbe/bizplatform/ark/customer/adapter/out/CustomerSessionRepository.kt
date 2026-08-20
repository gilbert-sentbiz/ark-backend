package com.sentbe.bizplatform.ark.customer.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CustomerSessionRepository : CrudRepository<CustomerSessionJdbcEntity, UUID> {
	fun findByToken(token: String): CustomerSessionJdbcEntity?
}
