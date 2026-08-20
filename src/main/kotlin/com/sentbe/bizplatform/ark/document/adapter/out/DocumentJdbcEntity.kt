package com.sentbe.bizplatform.ark.document.adapter.out

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Table("document")
data class DocumentJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("case_id") val caseId: UUID,
	@Column("doc_template_id") val docTemplateId: UUID,
	@Column("type") val type: String,
	@Column("display_name") val displayName: String,
	@Column("status") val status: String,
	@Column("is_required") val isRequired: Boolean,
	@Column("is_conditional") val isConditional: Boolean,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
	@LastModifiedDate @Column("updated_at") @ReadOnlyProperty val updatedAt: Instant? = null,
)

@Table("document_file")
data class DocumentFileJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("document_id") val documentId: UUID,
	@Column("file_name") val fileName: String,
	@Column("file_size") val fileSize: Int,
	@Column("mime_type") val mimeType: String,
	@Column("storage_key") val storageKey: String,
	@Column("uploader_type") val uploaderType: String,
	@Column("uploader_staff_id") val uploaderStaffId: UUID? = null,
	@Column("is_latest") val isLatest: Boolean = true,
	@CreatedDate @Column("uploaded_at") @ReadOnlyProperty val uploadedAt: Instant? = null,
)

@Table("revision_request")
data class RevisionRequestJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("document_id") val documentId: UUID,
	@Column("reason") val reason: String,
	@Column("requested_by_staff_id") val requestedByStaffId: UUID,
	@Column("requested_from_status") val requestedFromStatus: String,
	@CreatedDate @Column("requested_at") @ReadOnlyProperty val requestedAt: Instant? = null,
	@Column("resolved_at") val resolvedAt: OffsetDateTime? = null,
)
