package com.sentbe.bizplatform.ark.rule.adapter.out

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Table("segment")
data class SegmentJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("axis") val axis: String,
	@Column("code") val code: String,
	@Column("label") val label: String,
	// jsonb — read as raw String via PGobjectToStringConverter
	@Column("classification_trigger") val classificationTrigger: String? = null,
	@Column("question_overrides") val questionOverrides: String? = null,
	@Column("doc_overrides") val docOverrides: String? = null,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
	@Column("deactivated_at") val deactivatedAt: OffsetDateTime? = null,
)

@Table("question")
data class QuestionJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("code") val code: String,
	@Column("phase") val phase: String,
	@Column("classification") val classification: String,
	@Column("owner_segment_id") val ownerSegmentId: UUID? = null,
	@Column("label") val label: String,
	@Column("input_type") val inputType: String,
	// jsonb — raw String
	@Column("options") val options: String? = null,
	@Column("is_required") val isRequired: Boolean = false,
	@Column("show_when") val showWhen: String? = null,
	@Column("repeat") val repeat: Boolean = false,
	@Column("parent_question_id") val parentQuestionId: UUID? = null,
	@Column("display_order") val displayOrder: Int = 0,
	@Column("replaces_question_id") val replacesQuestionId: UUID? = null,
	@Column("created_by_staff_id") val createdByStaffId: UUID? = null,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
	@Column("deactivated_at") val deactivatedAt: OffsetDateTime? = null,
)

@Table("doc_template")
data class DocTemplateJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("type") val type: String,
	@Column("display_name") val displayName: String,
	@Column("classification") val classification: String,
	@Column("owner_segment_id") val ownerSegmentId: UUID? = null,
	@Column("is_required") val isRequired: Boolean = true,
	@Column("is_conditional") val isConditional: Boolean = false,
	// jsonb — raw String
	@Column("condition") val condition: String? = null,
	@Column("guide") val guide: String? = null,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
	@Column("deactivated_at") val deactivatedAt: OffsetDateTime? = null,
)
