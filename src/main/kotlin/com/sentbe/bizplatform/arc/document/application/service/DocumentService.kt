package com.sentbe.bizplatform.arc.document.application.service

import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.case.application.port.out.CaseOutPort
import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.domain.DocumentFile
import com.sentbe.bizplatform.arc.document.application.domain.RevisionRequest
import com.sentbe.bizplatform.arc.document.application.port.input.DocumentUseCase
import com.sentbe.bizplatform.arc.document.application.port.out.DocumentOutPort
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

private val ALLOWED_TYPES = setOf("application/pdf", "image/png", "image/jpeg")
private const val MAX_SIZE_BYTES = 10 * 1024 * 1024

@Service
class DocumentService(
	private val adapter: DocumentOutPort,
	private val storage: S3StorageService,
	private val casePort: CaseOutPort,
) : DocumentUseCase {
	override fun getDocuments(
		caseId: UUID,
		customer: AuthenticatedCustomer,
	): List<DocumentDetail> {
		val case = casePort.findById(caseId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "케이스를 찾을 수 없습니다")
		if (case.customerId != customer.id) throw ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다")
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
			throw ResponseStatusException(HttpStatus.CONFLICT, "이 상태에서는 파일을 업로드할 수 없습니다: ${doc.status}")
		}
		if (doc.status != "REVISION_REQUIRED" && adapter.hasLatestFile(documentId)) {
			throw ResponseStatusException(HttpStatus.CONFLICT, "이미 업로드된 파일이 있습니다. MVP는 서류당 1파일만 허용됩니다.")
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
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "반려할 수 없는 상태입니다: ${doc.status}")
		}
		if (reason.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유는 필수입니다")
		}

		val case =
			casePort.findById(doc.caseId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "케이스를 찾을 수 없습니다")
		if (case.status in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 케이스의 서류는 반려할 수 없습니다")
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
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "제출된 서류만 승인할 수 있습니다: ${doc.status}")
		}

		adapter.resolveOpenRevisions(documentId)
		adapter.updateStatus(documentId, "APPROVED")

		return adapter.findByCaseId(doc.caseId).first { it.document.id == documentId }
	}

	private fun requireDocument(documentId: UUID) =
		adapter.findById(documentId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다")

	private fun validateFile(file: MultipartFile) {
		if (file.contentType !in ALLOWED_TYPES) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "허용 형식: pdf, png, jpg")
		}
		if (file.size > MAX_SIZE_BYTES) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다")
		}
		if (file.isEmpty) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다")
		}
	}

	private fun requireRole(
		staff: AuthenticatedStaff,
		vararg roles: String,
	) {
		if (staff.role !in roles) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN, "이 작업에 필요한 역할: ${roles.joinToString()}")
		}
	}
}
