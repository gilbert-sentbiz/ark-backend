package com.sentbe.bizplatform.ark.case.adapter.`in`

import com.sentbe.bizplatform.ark.case.application.port.`in`.CasePort
import com.sentbe.bizplatform.ark.global.auth.AuthContext
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

data class CaseSummaryResponse(
	val id: UUID,
	val customerId: UUID,
	val companyName: String?,
	val status: String,
	val entityCode: String?,
	val services: List<String>,
	val assigneeStaffId: UUID?,
	val createdAt: OffsetDateTime,
	val updatedAt: OffsetDateTime,
)

data class InternalCaseDetailResponse(
	val id: UUID,
	val customerId: UUID,
	val status: String,
	val entityCode: String?,
	val services: List<String>,
	val segmentMeta: Map<String, Any>,
	val pinnedQuestionIds: Map<String, Any>,
	val assigneeStaffId: UUID?,
	val closeReason: String?,
	val revisionRequestedFrom: String?,
	val createdAt: OffsetDateTime,
	val updatedAt: OffsetDateTime,
	val timeline: List<Map<String, Any>>,
)

data class CloseBody(
	val reason: String,
)

@Tag(name = "Internal Case", description = "내부 케이스 관리 API")
@RestController
@RequestMapping("/internal/cases")
class InternalCaseAdapter(
	private val service: CasePort,
) {
	@Operation(summary = "I1 내부 케이스 목록")
	@GetMapping
	fun getCases(
		@RequestParam(required = false) status: String?,
	): List<CaseSummaryResponse> {
		requireStaff()
		val all = service.getAllCases()
		val filtered = if (status != null) all.filter { it.status == status } else all
		return filtered.map {
			val companyName =
				service
					.getIntake(it.id, "first")
					?.answers
					?.get("company_name") as? String
			CaseSummaryResponse(
				id = it.id,
				customerId = it.customerId,
				companyName = companyName,
				status = it.status,
				entityCode = it.entityCode,
				services = it.services,
				assigneeStaffId = it.assigneeStaffId,
				createdAt = it.createdAt,
				updatedAt = it.updatedAt,
			)
		}
	}

	@Operation(summary = "I2 내부 케이스 상세 + 타임라인")
	@GetMapping("/{id}")
	fun getCase(
		@PathVariable id: UUID,
	): InternalCaseDetailResponse {
		requireStaff()
		val case = service.getCase(id)
		val timeline = service.getCaseTimeline(id)
		return InternalCaseDetailResponse(
			id = case.id,
			customerId = case.customerId,
			status = case.status,
			entityCode = case.entityCode,
			services = case.services,
			segmentMeta = case.segmentMeta,
			pinnedQuestionIds = case.pinnedQuestionIds,
			assigneeStaffId = case.assigneeStaffId,
			closeReason = case.closeReason,
			revisionRequestedFrom = case.revisionRequestedFrom,
			createdAt = case.createdAt,
			updatedAt = case.updatedAt,
			timeline = timeline,
		)
	}

	@Operation(summary = "I3 검토 단계 전이")
	@PostMapping("/{id}/advance")
	fun advance(
		@PathVariable id: UUID,
	): CaseResponse {
		val staff = requireStaff()
		return service.advanceStatus(id, staff).toResponse()
	}

	@Operation(summary = "I4 케이스 종료")
	@PostMapping("/{id}/close")
	fun close(
		@PathVariable id: UUID,
		@RequestBody body: CloseBody,
	): CaseResponse {
		val staff = requireStaff()
		return service.closeCase(id, staff, body.reason).toResponse()
	}

	private fun requireStaff() =
		AuthContext.staff
			?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
}
