package com.sentbe.bizplatform.ark.rule.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Segment(
	val id: UUID,
	val axis: String,
	val code: String,
	val label: String,
	val classificationTrigger: List<Any>? = null,
	val questionOverrides: List<Any>? = null,
	val docOverrides: List<Any>? = null,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
	val deactivatedAt: OffsetDateTime? = null,
)
