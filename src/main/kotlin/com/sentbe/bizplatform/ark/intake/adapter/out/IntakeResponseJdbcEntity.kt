package com.sentbe.bizplatform.ark.intake.adapter.out

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("intake_response")
data class IntakeResponseJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("case_id") val caseId: UUID,
	@Column("phase") val phase: String,
	@Column("status") val status: String = "not_started",
	@Column("answers") val answers: Map<String, Any> = emptyMap(),
	@ReadOnlyProperty @Column("saved_at") val savedAt: OffsetDateTime? = null,
	@Column("submitted_at") val submittedAt: OffsetDateTime? = null,
)
