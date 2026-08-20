package com.sentbe.bizplatform.ark.document.application.service

import com.sentbe.bizplatform.ark.case.application.domain.CaseStatus
import com.sentbe.bizplatform.ark.case.application.port.out.CaseOutPort
import com.sentbe.bizplatform.ark.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.ark.document.application.domain.DocumentFile
import com.sentbe.bizplatform.ark.document.application.domain.RevisionRequest
import com.sentbe.bizplatform.ark.document.application.port.`in`.DocumentPort
import com.sentbe.bizplatform.ark.document.application.port.out.DocumentOutPort
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.util.UUID

private val ALLOWED_TYPES = setOf("application/pdf", "image/png", "image/jpeg")
private const val MAX_SIZE_BYTES = 10 * 1024 * 1024

@Service
class DocumentService(
	private val adapter: DocumentOutPort,
	private val storage: S3StorageService,
	private val casePort: CaseOutPort,
) : DocumentPort {
	override fun getDocuments(
		caseId: UUID,
		customer: AuthenticatedCustomer,
	): List<DocumentDetail> {
		val case = casePort.findById(caseId) ?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)
		if (case.customerId != customer.id) throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
		return adapter.findByCaseId(caseId)
	}

	@Transactional
	override fun uploadFile(
		documentId: UUID,
		file: MultipartFile,
		customer: AuthenticatedCustomer,
	): DocumentDetail {
		val doc = requireDocument(documentId)
		if (doc.status !in setOf("REQUESTED", "REVISION_REQUIRED")) {
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}
		if (doc.status != "REVISION_REQUIRED" && adapter.hasLatestFile(documentId)) {
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}
		validateFile(file)

		val caseId = doc.caseId
		val wasRevisionRequired = doc.status == "REVISION_REQUIRED"

		val ext = file.originalFilename?.substringAfterLast('.', "") ?: ""
		val key = "$caseId/$documentId/${UUID.randomUUID()}.$ext"
		storage.upload(key, file.bytes, file.contentType ?: "application/octet-stream")

		adapter.markPreviousFilesOld(documentId)
		adapter.insertFile(
			DocumentFile(
				id = UUID.randomUUID(),
				documentId = documentId,
				fileName = file.originalFilename ?: file.name,
				fileSize = file.size.toInt(),
				mimeType = file.contentType ?: "application/octet-stream",
				storageKey = key,
				uploaderType = "CUSTOMER",
				uploaderStaffId = null,
				isLatest = true,
				uploadedAt = OffsetDateTime.now(),
			),
		)

		if (wasRevisionRequired) {
			adapter.resolveOpenRevisions(documentId)
		}

		adapter.updateStatus(documentId, "SUBMITTED")

		val currentCase = casePort.findById(caseId)
		if (currentCase?.status == CaseStatus.DOCUMENT_SUBMISSION_REQUIRED &&
			!adapter.hasUnsubmittedRequiredDocs(caseId)
		) {
			casePort.save(currentCase.copy(status = CaseStatus.INITIAL_SCREENING))
		}

		return adapter.findByCaseId(caseId).first { it.document.id == documentId }
	}

	@Transactional
	override fun requestRevision(
		documentId: UUID,
		staff: AuthenticatedStaff,
		reason: String,
	): DocumentDetail {
		requireRole(staff, "OPS", "COMPLIANCE", "ADMIN", "SALES")
		val doc = requireDocument(documentId)
		if (doc.status !in setOf("SUBMITTED", "APPROVED")) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
		if (reason.isBlank()) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}

		val case =
			casePort.findById(doc.caseId)
				?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)
		if (case.status in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}

		val caseStatusForRevision =
			if (case.status == CaseStatus.REVISION_REQUESTED) {
				case.revisionRequestedFrom ?: case.status
			} else {
				case.status
			}

		adapter.insertRevisionRequest(
			RevisionRequest(
				id = UUID.randomUUID(),
				documentId = documentId,
				reason = reason,
				requestedByStaffId = staff.id,
				requestedFromStatus = caseStatusForRevision,
				requestedAt = OffsetDateTime.now(),
				resolvedAt = null,
			),
		)
		adapter.updateStatus(documentId, "REVISION_REQUIRED")

		if (case.status != CaseStatus.REVISION_REQUESTED) {
			casePort.save(
				case.copy(
					status = CaseStatus.REVISION_REQUESTED,
					revisionRequestedFrom = case.status,
				),
			)
		}

		return adapter.findByCaseId(doc.caseId).first { it.document.id == documentId }
	}

	@Transactional
	override fun approveDocument(
		documentId: UUID,
		staff: AuthenticatedStaff,
	): DocumentDetail {
		requireRole(staff, "COMPLIANCE")
		val doc = requireDocument(documentId)
		if (doc.status != "SUBMITTED") {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}

		adapter.resolveOpenRevisions(documentId)
		adapter.updateStatus(documentId, "APPROVED")

		return adapter.findByCaseId(doc.caseId).first { it.document.id == documentId }
	}

	private fun requireDocument(documentId: UUID) =
		adapter.findById(documentId)
			?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)

	private fun validateFile(file: MultipartFile) {
		if (file.contentType !in ALLOWED_TYPES) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
		if (file.size > MAX_SIZE_BYTES) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
		if (file.isEmpty) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
	}

	private fun requireRole(
		staff: AuthenticatedStaff,
		vararg roles: String,
	) {
		if (staff.role !in roles) {
			throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
		}
	}
}
