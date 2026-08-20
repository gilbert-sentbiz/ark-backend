package com.sentbe.bizplatform.ark.case.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class IntakeResponse(
	val id: UUID = UUID.randomUUID(),
	val caseId: UUID,
	val phase: String,
	val status: String = "not_started",
	val answers: Map<String, Any> = emptyMap(),
	val savedAt: OffsetDateTime = OffsetDateTime.now(),
	val submittedAt: OffsetDateTime? = null,
)
