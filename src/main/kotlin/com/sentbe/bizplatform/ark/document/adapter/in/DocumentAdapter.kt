package com.sentbe.bizplatform.ark.document.adapter.`in`

import com.sentbe.bizplatform.ark.document.application.domain.DocumentDetail
import com.sentbe.bizplatform.ark.document.application.port.`in`.DocumentPort
import com.sentbe.bizplatform.ark.global.auth.AuthContext
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.util.UUID

data class DocumentFileDto(
	val fileName: String,
	val mimeType: String,
	val uploadedAt: OffsetDateTime,
)

data class RevisionDto(
	val id: UUID,
	val reason: String,
	val requestedAt: OffsetDateTime,
)

data class DocumentResponse(
	val id: UUID,
	val caseId: UUID,
	val type: String,
	val displayName: String,
	val status: String,
	val isRequired: Boolean,
	val latestFile: DocumentFileDto?,
	val openRevisions: List<RevisionDto>,
)

@Tag(name = "Document", description = "케이스 서류 API")
@RestController
@RequestMapping("/cases/{caseId}/documents")
class DocumentAdapter(
	private val service: DocumentPort,
) {
	@Operation(summary = "C9 케이스 서류 목록 조회")
	@GetMapping
	fun listDocuments(
		@PathVariable caseId: UUID,
	): List<DocumentResponse> {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.getDocuments(caseId, customer).map { it.toResponse() }
	}

	@Operation(summary = "C10 서류 파일 업로드")
	@PostMapping("/{docId}/file")
	fun uploadFile(
		@PathVariable caseId: UUID,
		@PathVariable docId: UUID,
		@RequestParam("file") file: MultipartFile,
	): DocumentResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.uploadFile(docId, file, customer).toResponse()
	}
}

internal fun DocumentDetail.toResponse() =
	DocumentResponse(
		id = document.id,
		caseId = document.caseId,
		type = document.type,
		displayName = document.displayName,
		status = document.status,
		isRequired = document.isRequired,
		latestFile =
			latestFile?.let {
				DocumentFileDto(
					fileName = it.fileName,
					mimeType = it.mimeType,
					uploadedAt = it.uploadedAt,
				)
			},
		openRevisions =
			openRevisions.map { r ->
				RevisionDto(
					id = r.id,
					reason = r.reason,
					requestedAt = r.requestedAt,
				)
			},
	)
