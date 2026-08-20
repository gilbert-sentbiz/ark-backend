package com.sentbe.bizplatform.ark.staff.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class StaffSession(
	val id: UUID? = null,
	val staffId: UUID,
	val token: String,
	val expiresAt: OffsetDateTime,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
