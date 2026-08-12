package com.sentbe.bizplatform.arc.document.application.service

import com.sentbe.bizplatform.arc.document.adapter.out.DocumentJdbcAdapter
import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.domain.DocumentFile
import com.sentbe.bizplatform.arc.document.application.domain.RevisionRequest
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

private val ALLOWED_TYPES = setOf("application/pdf", "image/png", "image/jpeg")
private const val MAX_SIZE_BYTES = 10 * 1024 * 1024

@Service
class DocumentService(
    private val adapter: DocumentJdbcAdapter,
    private val storage: S3StorageService,
) {
    fun getDocuments(
        caseId: UUID,
        customer: AuthenticatedCustomer,
    ): List<DocumentDetail> = adapter.findByCaseId(caseId)

    @Transactional
    fun uploadFile(
        documentId: UUID,
        file: MultipartFile,
        customer: AuthenticatedCustomer,
    ): DocumentDetail {
        val doc = requireDocument(documentId)
        if (doc.status !in setOf("REQUESTED", "REVISION_REQUIRED")) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이 상태에서는 파일을 업로드할 수 없습니다: ${doc.status}")
        }
        validateFile(file)

        val ext = file.originalFilename?.substringAfterLast('.', "") ?: ""
        val key = "${doc.caseId}/$documentId/${UUID.randomUUID()}.$ext"
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
                uploadedAt = java.time.OffsetDateTime.now(),
            ),
        )

        if (doc.status == "REVISION_REQUIRED") {
            adapter.resolveOpenRevisions(documentId)
        }
        adapter.updateStatus(documentId, "SUBMITTED")

        return adapter.findByCaseId(doc.caseId).first { it.document.id == documentId }
    }

    @Transactional
    fun requestRevision(
        documentId: UUID,
        staff: AuthenticatedStaff,
        reason: String,
    ): DocumentDetail {
        requireRole(staff, "OPS", "COMPLIANCE", "ADMIN")
        val doc = requireDocument(documentId)
        if (doc.status !in setOf("SUBMITTED", "APPROVED")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "반려할 수 없는 상태입니다: ${doc.status}")
        }
        if (reason.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유는 필수입니다")
        }

        adapter.insertRevisionRequest(
            RevisionRequest(
                id = UUID.randomUUID(),
                documentId = documentId,
                reason = reason,
                requestedByStaffId = staff.id,
                requestedFromStatus = doc.status,
                requestedAt = java.time.OffsetDateTime.now(),
                resolvedAt = null,
            ),
        )
        adapter.updateStatus(documentId, "REVISION_REQUIRED")

        return adapter.findByCaseId(doc.caseId).first { it.document.id == documentId }
    }

    @Transactional
    fun approveDocument(
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
