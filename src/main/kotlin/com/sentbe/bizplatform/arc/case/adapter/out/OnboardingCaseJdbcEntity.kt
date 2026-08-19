package com.sentbe.bizplatform.arc.case.adapter.out

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("onboarding_case")
data class OnboardingCaseJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("customer_id") val customerId: UUID,
	@Column("status") val status: String,
	@Column("close_reason") val closeReason: String? = null,
	@Column("revision_requested_from") val revisionRequestedFrom: String? = null,
	@Column("entity_code") val entityCode: String? = null,
	// text[] — read via ArrayToStringListConverter; writes use JdbcClient (::text[] cast required)
	@Column("services") val services: List<String> = emptyList(),
	@Column("sectors") val sectors: List<String> = emptyList(),
	// jsonb — read via JsonbToMapConverter; write via MapToJsonbConverter
	@Column("segment_meta") val segmentMeta: Map<String, Any> = emptyMap(),
	@Column("pinned_question_ids") val pinnedQuestionIds: Map<String, Any> = emptyMap(),
	@Column("assignee_staff_id") val assigneeStaffId: UUID? = null,
	@Column("last_customer_action_at") val lastCustomerActionAt: OffsetDateTime? = null,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: OffsetDateTime? = null,
	@LastModifiedDate @Column("updated_at") @ReadOnlyProperty val updatedAt: OffsetDateTime? = null,
)

@Table("case_event")
data class CaseEventJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("case_id") val caseId: UUID,
	@Column("event_type") val eventType: String,
	@Column("actor_type") val actorType: String,
	@Column("actor_id") val actorId: UUID? = null,
	@Column("payload") val payload: Map<String, Any> = emptyMap(),
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: OffsetDateTime? = null,
)
