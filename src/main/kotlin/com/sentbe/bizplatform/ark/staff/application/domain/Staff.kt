package com.sentbe.bizplatform.ark.staff.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Staff(
	val id: UUID,
	val email: String,
	val name: String,
	val role: String,
	val isActive: Boolean = true,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
