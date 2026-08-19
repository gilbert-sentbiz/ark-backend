package com.sentbe.bizplatform.arc.customer.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("customer")
data class Customer(
	@Id val id: UUID? = null,
	val email: String,
	val authMethod: String = "otp",
	val passwordHash: String? = null,
	val businessRegNo: String? = null,
	val companyName: String? = null,
	val contactName: String? = null,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
