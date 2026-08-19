package com.sentbe.bizplatform.arc.document.adapter.out

import com.sentbe.bizplatform.arc.document.application.domain.Document
import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.domain.DocumentFile
import com.sentbe.bizplatform.arc.document.application.domain.RevisionRequest
import com.sentbe.bizplatform.arc.document.application.port.out.DocumentOutPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class DocumentOutAdapter(
	private val jdbc: JdbcClient,
	private val documentRepository: DocumentRepository,
	private val documentFileRepository: DocumentFileRepository,
	private val revisionRequestRepository: RevisionRequestRepository,
) : DocumentOutPort {
	override fun findById(id: UUID): Document? = documentRepository.findById(id).orElse(null)?.toDomain()

	override fun findByCaseId(caseId: UUID): List<DocumentDetail> =
		documentRepository.findByCaseIdOrderByType(caseId).map { entity ->
			val latestFile = documentFileRepository.findFirstByDocumentIdAndIsLatestTrue(entity.id)?.toDomain()
			val openRevisions =
				revisionRequestRepository
					.findByDocumentIdAndResolvedAtIsNullOrderByRequestedAt(entity.id)
					.map { it.toDomain() }
			DocumentDetail(entity.toDomain(), latestFile, openRevisions)
		}

	override fun updateStatus(
		id: UUID,
		status: String,
	) {
		jdbc
			.sql("UPDATE document SET status = :status, updated_at = now() WHERE id = :id")
			.param("id", id)
			.param("status", status)
			.update()
	}

	override fun markPreviousFilesOld(documentId: UUID) {
		jdbc
			.sql("UPDATE document_file SET is_latest = false WHERE document_id = :documentId AND is_latest = true")
			.param("documentId", documentId)
			.update()
	}

	override fun insertFile(file: DocumentFile) {
		jdbc
			.sql(
				"""INSERT INTO document_file
               (id, document_id, file_name, file_size, mime_type, storage_key,
                uploader_type, uploader_staff_id, is_latest)
               VALUES (:id, :documentId, :fileName, :fileSize, :mimeType, :storageKey,
                       :uploaderType, :uploaderStaffId, true)""",
			).param("id", file.id)
			.param("documentId", file.documentId)
			.param("fileName", file.fileName)
			.param("fileSize", file.fileSize)
			.param("mimeType", file.mimeType)
			.param("storageKey", file.storageKey)
			.param("uploaderType", file.uploaderType)
			.param("uploaderStaffId", file.uploaderStaffId)
			.update()
	}

	override fun insertRevisionRequest(revision: RevisionRequest) {
		jdbc
			.sql(
				"""INSERT INTO revision_request
               (id, document_id, reason, requested_by_staff_id, requested_from_status)
               VALUES (:id, :documentId, :reason, :staffId, :fromStatus)""",
			).param("id", revision.id)
			.param("documentId", revision.documentId)
			.param("reason", revision.reason)
			.param("staffId", revision.requestedByStaffId)
			.param("fromStatus", revision.requestedFromStatus)
			.update()
	}

	override fun resolveOpenRevisions(documentId: UUID) {
		jdbc
			.sql("UPDATE revision_request SET resolved_at = now() WHERE document_id = :documentId AND resolved_at IS NULL")
			.param("documentId", documentId)
			.update()
	}

	override fun hasUnsubmittedRequiredDocs(caseId: UUID): Boolean {
		val count =
			jdbc
				.sql("SELECT COUNT(*) FROM document WHERE case_id = :caseId AND is_required = true AND status NOT IN ('SUBMITTED', 'APPROVED')")
				.param("caseId", caseId)
				.query(Int::class.java)
				.single()
		return count > 0
	}

	override fun countOpenRevisionsByCaseId(caseId: UUID): Int =
		jdbc
			.sql(
				"""SELECT COUNT(*) FROM revision_request rr
               JOIN document d ON rr.document_id = d.id
               WHERE d.case_id = :caseId AND rr.resolved_at IS NULL""",
			).param("caseId", caseId)
			.query(Int::class.java)
			.single()

	override fun hasLatestFile(documentId: UUID): Boolean = documentFileRepository.findFirstByDocumentIdAndIsLatestTrue(documentId) != null

	private fun DocumentJdbcEntity.toDomain() =
		Document(
			id = id,
			caseId = caseId,
			docTemplateId = docTemplateId,
			type = type,
			displayName = displayName,
			status = status,
			isRequired = isRequired,
			isConditional = isConditional,
			createdAt = createdAt ?: OffsetDateTime.now(),
			updatedAt = updatedAt ?: OffsetDateTime.now(),
		)

	private fun DocumentFileJdbcEntity.toDomain() =
		DocumentFile(
			id = id!!,
			documentId = documentId,
			fileName = fileName,
			fileSize = fileSize,
			mimeType = mimeType,
			storageKey = storageKey,
			uploaderType = uploaderType,
			uploaderStaffId = uploaderStaffId,
			isLatest = isLatest,
			uploadedAt = uploadedAt ?: OffsetDateTime.now(),
		)

	private fun RevisionRequestJdbcEntity.toDomain() =
		RevisionRequest(
			id = id!!,
			documentId = documentId,
			reason = reason,
			requestedByStaffId = requestedByStaffId,
			requestedFromStatus = requestedFromStatus,
			requestedAt = requestedAt ?: OffsetDateTime.now(),
			resolvedAt = resolvedAt,
		)
}
