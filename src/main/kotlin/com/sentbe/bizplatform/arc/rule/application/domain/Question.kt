package com.sentbe.bizplatform.arc.rule.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("question")
data class Question(
	@Id val id: UUID,
	val code: String,
	val phase: String,
	val classification: String,
	val ownerSegmentId: UUID? = null,
	val label: String,
	val inputType: String,
	val options: List<Any>? = null,
	val isRequired: Boolean = false,
	val showWhen: Map<String, Any>? = null,
	val repeat: Boolean = false,
	val parentQuestionId: UUID? = null,
	val displayOrder: Int = 0,
	val replacesQuestionId: UUID? = null,
	val createdByStaffId: UUID? = null,
	val createdAt: OffsetDateTime = OffsetDateTime.now(),
	val deactivatedAt: OffsetDateTime? = null,
)
