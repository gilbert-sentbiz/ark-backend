package com.sentbe.bizplatform.arc.document.application.port.input

import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedCustomer
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface DocumentUseCase {
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
