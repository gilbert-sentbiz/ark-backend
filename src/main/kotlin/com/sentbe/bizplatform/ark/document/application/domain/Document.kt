package com.sentbe.bizplatform.ark.document.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class Document(
	val id: UUID,
	val caseId: UUID,
	val docTemplateId: UUID,
	val type: String,
	val displayName: String,
	val status: String,
	val isRequired: Boolean,
	val isConditional: Boolean,
	val createdAt: OffsetDateTime,
	val updatedAt: OffsetDateTime,
)

data class DocumentFile(
	val id: UUID,
	val documentId: UUID,
	val fileName: String,
	val fileSize: Int,
	val mimeType: String,
	val storageKey: String,
	val uploaderType: String,
	val uploaderStaffId: UUID?,
	val isLatest: Boolean,
	val uploadedAt: OffsetDateTime,
)

data class RevisionRequest(
	val id: UUID,
	val documentId: UUID,
	val reason: String,
	val requestedByStaffId: UUID,
	val requestedFromStatus: String,
	val requestedAt: OffsetDateTime,
	val resolvedAt: OffsetDateTime?,
)

data class DocumentDetail(
	val document: Document,
	val latestFile: DocumentFile?,
	val openRevisions: List<RevisionRequest>,
)
