package com.sentbe.bizplatform.arc.document.adapter.input

import com.sentbe.bizplatform.arc.document.application.port.input.DocumentUseCase
import com.sentbe.bizplatform.arc.global.auth.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class RevisionRequestBody(
    val reason: String,
)

@RestController
@RequestMapping("/internal/documents")
class InternalDocumentController(
    private val service: DocumentUseCase,
) {
    @PostMapping("/{id}/revision-requests")
    fun requestRevision(
        @PathVariable id: UUID,
        @RequestBody body: RevisionRequestBody,
    ): Map<String, Any> {
        val staff =
            AuthContext.staff
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "직원 인증이 필요합니다")
        return service.requestRevision(id, staff, body.reason).toMap()
    }

    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
    ): Map<String, Any> {
        val staff =
            AuthContext.staff
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "직원 인증이 필요합니다")
        return service.approveDocument(id, staff).toMap()
    }
}
