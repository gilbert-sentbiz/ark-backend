package com.sentbe.bizplatform.ark.document.application.port.`in`

import com.sentbe.bizplatform.ark.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedStaff
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface DocumentPort {
	fun getDocuments(
		caseId: UUID,
		customer: AuthenticatedCustomer,
	): List<DocumentDetail>

	fun uploadFile(
		documentId: UUID,
		file: MultipartFile,
		customer: AuthenticatedCustomer,
	): DocumentDetail

	fun requestRevision(
		documentId: UUID,
		staff: AuthenticatedStaff,
		reason: String,
	): DocumentDetail

	fun approveDocument(
		documentId: UUID,
		staff: AuthenticatedStaff,
	): DocumentDetail
}
