package com.sentbe.bizplatform.ark.document.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface DocumentRepository : CrudRepository<DocumentJdbcEntity, UUID> {
	fun findByCaseIdOrderByType(caseId: UUID): List<DocumentJdbcEntity>
}

interface DocumentFileRepository : CrudRepository<DocumentFileJdbcEntity, UUID> {
	fun findFirstByDocumentIdAndIsLatestTrue(documentId: UUID): DocumentFileJdbcEntity?

	fun findByDocumentId(documentId: UUID): List<DocumentFileJdbcEntity>
}

interface RevisionRequestRepository : CrudRepository<RevisionRequestJdbcEntity, UUID> {
	fun findByDocumentIdAndResolvedAtIsNullOrderByRequestedAt(documentId: UUID): List<RevisionRequestJdbcEntity>
}
