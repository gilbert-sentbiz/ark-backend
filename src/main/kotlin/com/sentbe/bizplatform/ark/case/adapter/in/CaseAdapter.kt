package com.sentbe.bizplatform.ark.case.adapter.`in`

import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.ark.case.application.port.`in`.CasePort
import com.sentbe.bizplatform.ark.global.auth.AuthContext
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

data class IntakeAnswersRequest(
	val answers: Map<String, Any>,
)

data class CaseResponse(
	val id: UUID,
	val status: String,
	val entityCode: String?,
	val services: List<String>,
	val closeReason: String?,
	val revisionRequestedFrom: String?,
	val pinnedQuestionIds: Map<String, Any>,
	val createdAt: OffsetDateTime,
	val updatedAt: OffsetDateTime,
)

@Tag(name = "Case", description = "고객 케이스 API")
@RestController
@RequestMapping("/cases")
class CaseAdapter(
	private val service: CasePort,
) {
	@Operation(summary = "C1 케이스 생성", description = "고객 OTP 세션으로 케이스를 생성한다. 1계정 1활성 케이스; 위반 시 409.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun createCase(): CaseResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.createCase(customer.id).toResponse()
	}

	@Operation(summary = "C2b 본인 케이스 조회", description = "고객 토큰으로 본인 활성 케이스를 id 없이 조회. 재로그인/타 기기 복귀용. 없으면 204.")
	@GetMapping("/me")
	fun getMyCase(): ResponseEntity<CaseResponse> {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		val case = service.findMyCase(customer.id)
		// 없으면 204 No Content — 200+빈 본문으로 인한 프론트 json 파싱 에러 방지
		return case?.let { ResponseEntity.ok(it.toResponse()) } ?: ResponseEntity.noContent().build()
	}

	@Operation(summary = "C2 케이스 상세")
	@GetMapping("/{id}")
	fun getCase(
		@PathVariable id: UUID,
	): CaseResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		val case = service.getCase(id)
		if (case.customerId != customer.id) {
			throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
		}
		return case.toResponse()
	}

	// C3 PUT /intake/first — MVP 제외 (임시저장 Full Spec, 2026-08-15 확정)

	@Operation(summary = "C4 1차 제출 → 분류")
	@PostMapping("/{id}/intake/first/submit")
	fun submitFirstIntake(
		@PathVariable id: UUID,
		@RequestBody body: IntakeAnswersRequest,
	): CaseResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.submitFirstIntake(id, customer.id, body.answers).toResponse()
	}

	// C5 PUT /intake/second — MVP 제외 (임시저장 Full Spec, 2026-08-15 확정)

	@Operation(summary = "C6 2차 제출 → 서류 생성")
	@PostMapping("/{id}/intake/second/submit")
	fun submitSecondIntake(
		@PathVariable id: UUID,
		@RequestBody body: IntakeAnswersRequest,
	): CaseResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.submitSecondIntake(id, customer.id, body.answers).toResponse()
	}

	@Operation(summary = "C7 저장된 인테이크 응답 조회")
	@GetMapping("/{id}/intake/{phase}")
	fun getIntake(
		@PathVariable id: UUID,
		@PathVariable phase: String,
	): IntakeDto {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		val case = service.getCase(id)
		if (case.customerId != customer.id) {
			throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
		}
		val intake =
			service.getIntake(id, phase)
				?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)
		return IntakeDto(
			caseId = intake.caseId,
			phase = intake.phase,
			status = intake.status,
			answers = intake.answers,
			savedAt = intake.savedAt,
			submittedAt = intake.submittedAt,
		)
	}

	@Operation(summary = "C8 보완 재제출")
	@PostMapping("/{id}/resubmit")
	fun resubmit(
		@PathVariable id: UUID,
	): CaseResponse {
		val customer =
			AuthContext.customer
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		return service.resubmit(id, customer.id).toResponse()
	}
}

data class IntakeDto(
	val caseId: UUID,
	val phase: String,
	val status: String,
	val answers: Map<String, Any>,
	val savedAt: OffsetDateTime,
	val submittedAt: OffsetDateTime?,
)

internal fun OnboardingCase.toResponse() =
	CaseResponse(
		id = id,
		status = status,
		entityCode = entityCode,
		services = services,
		closeReason = closeReason,
		revisionRequestedFrom = revisionRequestedFrom,
		pinnedQuestionIds = pinnedQuestionIds,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)
