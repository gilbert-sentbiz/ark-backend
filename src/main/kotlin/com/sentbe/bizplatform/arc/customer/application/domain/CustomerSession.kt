package com.sentbe.bizplatform.arc.customer.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class CustomerSession(
	val id: UUID? = null,
	val customerId: UUID,
	val token: String,
	val expiresAt: OffsetDateTime,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
