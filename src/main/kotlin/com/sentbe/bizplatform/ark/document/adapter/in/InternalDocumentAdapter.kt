package com.sentbe.bizplatform.ark.document.adapter.`in`

import com.sentbe.bizplatform.ark.document.application.port.`in`.DocumentPort
import com.sentbe.bizplatform.ark.global.auth.AuthContext
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class RevisionRequestBody(
	val reason: String,
)

@Tag(name = "Internal Document", description = "내부 서류 관리 API")
@RestController
@RequestMapping("/internal/documents")
class InternalDocumentAdapter(
	private val service: DocumentPort,
) {
	@Operation(summary = "I5 서류 보완 요청")
	@PostMapping("/{id}/revision-requests")
	fun requestRevision(
		@PathVariable id: UUID,
		@RequestBody body: RevisionRequestBody,
	): DocumentResponse {
		val staff =
			AuthContext.staff
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.requestRevision(id, staff, body.reason).toResponse()
	}

	@Operation(summary = "I6 서류 승인")
	@PostMapping("/{id}/approve")
	fun approve(
		@PathVariable id: UUID,
	): DocumentResponse {
		val staff =
			AuthContext.staff
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.approveDocument(id, staff).toResponse()
	}
}
