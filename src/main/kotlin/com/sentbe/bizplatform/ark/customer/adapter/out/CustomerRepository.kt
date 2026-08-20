package com.sentbe.bizplatform.ark.customer.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CustomerRepository : CrudRepository<CustomerJdbcEntity, UUID> {
	fun findByEmail(email: String): CustomerJdbcEntity?
}
