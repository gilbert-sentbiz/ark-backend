package com.sentbe.bizplatform.ark.customer.application.port.out

import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.customer.application.domain.CustomerSession

interface CustomerOutPort {
	fun findByEmail(email: String): Customer?

	fun saveCustomer(customer: Customer): Customer

	fun saveSession(session: CustomerSession)
}
