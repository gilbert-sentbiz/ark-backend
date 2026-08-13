package com.sentbe.bizplatform.arc.document.adapter.out

import com.sentbe.bizplatform.arc.document.application.domain.Document
import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.domain.DocumentFile
import com.sentbe.bizplatform.arc.document.application.domain.RevisionRequest
import com.sentbe.bizplatform.arc.document.application.port.out.DocumentOutPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

@Component
class DocumentJdbcAdapter(
    private val jdbc: JdbcClient,
) : DocumentOutPort {
    override fun findById(id: UUID): Document? =
        jdbc
            .sql("SELECT * FROM document WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> rs.toDocument() }
            .optional()
            .orElse(null)

    override fun findByCaseId(caseId: UUID): List<DocumentDetail> {
        val docs =
            jdbc
                .sql("SELECT * FROM document WHERE case_id = :caseId ORDER BY type")
                .param("caseId", caseId)
                .query { rs, _ -> rs.toDocument() }
                .list()

        return docs.map { doc ->
            val latestFile =
                jdbc
                    .sql("SELECT * FROM document_file WHERE document_id = :docId AND is_latest = true LIMIT 1")
                    .param("docId", doc.id)
                    .query { rs, _ -> rs.toFile() }
                    .optional()
                    .orElse(null)

            val openRevisions =
                jdbc
                    .sql("SELECT * FROM revision_request WHERE document_id = :docId AND resolved_at IS NULL ORDER BY requested_at")
                    .param("docId", doc.id)
                    .query { rs, _ -> rs.toRevision() }
                    .list()

            DocumentDetail(doc, latestFile, openRevisions)
        }
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
                .sql(
                    "SELECT COUNT(*) FROM document WHERE case_id = :caseId AND is_required = true AND status NOT IN ('SUBMITTED', 'APPROVED')",
                ).param("caseId", caseId)
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

    override fun hasLatestFile(documentId: UUID): Boolean {
        val count =
            jdbc
                .sql("SELECT COUNT(*) FROM document_file WHERE document_id = :documentId AND is_latest = true")
                .param("documentId", documentId)
                .query(Int::class.java)
                .single()
        return count > 0
    }

    private fun ResultSet.toDocument(): Document =
        Document(
            id = UUID.fromString(getString("id")),
            caseId = UUID.fromString(getString("case_id")),
            docTemplateId = UUID.fromString(getString("doc_template_id")),
            type = getString("type"),
            displayName = getString("display_name"),
            status = getString("status"),
            isRequired = getBoolean("is_required"),
            isConditional = getBoolean("is_conditional"),
            createdAt = getObject("created_at", OffsetDateTime::class.java),
            updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        )

    private fun ResultSet.toFile(): DocumentFile =
        DocumentFile(
            id = UUID.fromString(getString("id")),
            documentId = UUID.fromString(getString("document_id")),
            fileName = getString("file_name"),
            fileSize = getInt("file_size"),
            mimeType = getString("mime_type"),
            storageKey = getString("storage_key"),
            uploaderType = getString("uploader_type"),
            uploaderStaffId = getString("uploader_staff_id")?.let { UUID.fromString(it) },
            isLatest = getBoolean("is_latest"),
            uploadedAt = getObject("uploaded_at", OffsetDateTime::class.java),
        )

    private fun ResultSet.toRevision(): RevisionRequest =
        RevisionRequest(
            id = UUID.fromString(getString("id")),
            documentId = UUID.fromString(getString("document_id")),
            reason = getString("reason"),
            requestedByStaffId = UUID.fromString(getString("requested_by_staff_id")),
            requestedFromStatus = getString("requested_from_status"),
            requestedAt = getObject("requested_at", OffsetDateTime::class.java),
            resolvedAt = getObject("resolved_at", OffsetDateTime::class.java),
        )
}
