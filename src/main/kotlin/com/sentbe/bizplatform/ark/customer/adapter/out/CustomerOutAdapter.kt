package com.sentbe.bizplatform.ark.customer.adapter.out

import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.ark.customer.application.port.out.CustomerOutPort
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class CustomerOutAdapter(
	private val customerRepo: CustomerRepository,
	private val sessionRepo: CustomerSessionRepository,
) : CustomerOutPort {
	override fun findByEmail(email: String): Customer? = customerRepo.findByEmail(email)?.toDomain()

	override fun saveCustomer(customer: Customer): Customer = customerRepo.save(customer.toEntity()).toDomain()

	override fun saveSession(session: CustomerSession) {
		sessionRepo.save(session.toEntity())
	}

	private fun CustomerJdbcEntity.toDomain() =
		Customer(
			id = id,
			email = email,
			authMethod = authMethod,
			passwordHash = passwordHash,
			businessRegNo = businessRegNo,
			companyName = companyName,
			contactName = contactName,
			createdAt = createdAt ?: OffsetDateTime.now(),
		)

	private fun Customer.toEntity() =
		CustomerJdbcEntity(
			id = id,
			email = email,
			authMethod = authMethod,
			passwordHash = passwordHash,
			businessRegNo = businessRegNo,
			companyName = companyName,
			contactName = contactName,
		)

	private fun CustomerSessionJdbcEntity.toDomain() =
		CustomerSession(
			id = id,
			customerId = customerId,
			token = token,
			expiresAt = expiresAt,
			createdAt = createdAt ?: OffsetDateTime.now(),
		)

	private fun CustomerSession.toEntity() =
		CustomerSessionJdbcEntity(
			id = id,
			customerId = customerId,
			token = token,
			expiresAt = expiresAt,
		)
}
