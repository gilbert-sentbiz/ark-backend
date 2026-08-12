package com.sentbe.bizplatform.arc.document.adapter.`in`

import com.sentbe.bizplatform.arc.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.arc.document.application.port.`in`.DocumentUseCase
import com.sentbe.bizplatform.arc.global.auth.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/cases/{caseId}/documents")
class DocumentController(
    private val service: DocumentUseCase,
) {
    @GetMapping
    fun listDocuments(
        @PathVariable caseId: UUID,
    ): List<Map<String, Any>> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        return service.getDocuments(caseId, customer).map { it.toMap() }
    }

    @PostMapping("/{docId}/file")
    fun uploadFile(
        @PathVariable caseId: UUID,
        @PathVariable docId: UUID,
        @RequestParam("file") file: MultipartFile,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        return service.uploadFile(docId, file, customer).toMap()
    }
}

fun DocumentDetail.toMap(): Map<String, Any> =
    mapOf(
        "id" to document.id,
        "type" to document.type,
        "displayName" to document.displayName,
        "status" to document.status,
        "isRequired" to document.isRequired,
        "latestFile" to (
            latestFile?.let {
                mapOf("fileName" to it.fileName, "mimeType" to it.mimeType, "uploadedAt" to it.uploadedAt)
            } ?: emptyMap<String, Any>()
        ),
        "openRevisions" to
            openRevisions.map { r ->
                mapOf("id" to r.id, "reason" to r.reason, "requestedAt" to r.requestedAt)
            },
    )
