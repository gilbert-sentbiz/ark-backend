package com.sentbe.bizplatform.arc.customer.adapter.out

import com.sentbe.bizplatform.arc.customer.application.domain.Customer
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CustomerRepository : CrudRepository<Customer, UUID> {
    fun findByEmail(email: String): Customer?
}
