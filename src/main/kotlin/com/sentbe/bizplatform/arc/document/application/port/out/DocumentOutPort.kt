package com.sentbe.bizplatform.arc.document.application.port.out

import com.sentbe.bizplatform.arc.document.application.domain.Document
import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.domain.DocumentFile
import com.sentbe.bizplatform.arc.document.application.domain.RevisionRequest
import java.util.UUID

interface DocumentOutPort {
	fun findById(id: UUID): Document?

	fun findByCaseId(caseId: UUID): List<DocumentDetail>

	fun updateStatus(
		id: UUID,
		status: String,
	)

	fun markPreviousFilesOld(documentId: UUID)

	fun insertFile(file: DocumentFile)

	fun insertRevisionRequest(revision: RevisionRequest)

	fun resolveOpenRevisions(documentId: UUID)

	fun hasUnsubmittedRequiredDocs(caseId: UUID): Boolean

	fun countOpenRevisionsByCaseId(caseId: UUID): Int

	fun hasLatestFile(documentId: UUID): Boolean
}
