package com.sentbe.bizplatform.arc.rule.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class DocTemplate(
	val id: UUID,
	val type: String,
	val displayName: String,
	val classification: String,
	val ownerSegmentId: UUID? = null,
	val isRequired: Boolean = true,
	val isConditional: Boolean = false,
	val condition: Map<String, Any>? = null,
	val guide: String? = null,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
	val deactivatedAt: OffsetDateTime? = null,
)
