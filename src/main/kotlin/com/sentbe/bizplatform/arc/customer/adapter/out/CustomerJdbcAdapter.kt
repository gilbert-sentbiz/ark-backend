package com.sentbe.bizplatform.arc.customer.adapter.out

import com.sentbe.bizplatform.arc.customer.application.domain.Customer
import com.sentbe.bizplatform.arc.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.arc.customer.application.port.out.CustomerOutPort
import org.springframework.stereotype.Component

@Component
class CustomerJdbcAdapter(
    private val customerRepo: CustomerRepository,
    private val sessionRepo: CustomerSessionRepository,
) : CustomerOutPort {
    override fun findByEmail(email: String): Customer? = customerRepo.findByEmail(email)

    override fun saveCustomer(customer: Customer): Customer = customerRepo.save(customer)

    override fun saveSession(session: CustomerSession) {
        sessionRepo.save(session)
    }
}
