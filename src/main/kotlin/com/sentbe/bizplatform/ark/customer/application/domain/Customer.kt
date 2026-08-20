package com.sentbe.bizplatform.ark.customer.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Customer(
	val id: UUID? = null,
	val email: String,
	val authMethod: String = "otp",
	val passwordHash: String? = null,
	val businessRegNo: String? = null,
	val companyName: String? = null,
	val contactName: String? = null,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
