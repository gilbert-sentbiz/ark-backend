package com.sentbe.bizplatform.ark.rule.adapter.input

import com.sentbe.bizplatform.ark.global.auth.AuthContext
import com.sentbe.bizplatform.ark.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.ark.rule.application.domain.Question
import com.sentbe.bizplatform.ark.rule.application.domain.Segment
import com.sentbe.bizplatform.ark.rule.application.port.input.RuleUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class SegmentDto(
	val id: UUID,
	val axis: String,
	val code: String,
	val label: String,
)

data class QuestionDto(
	val id: UUID,
	val code: String,
	val phase: String,
	val classification: String,
	val ownerSegmentId: UUID?,
	val label: String,
	val inputType: String,
	val options: List<Any>?,
	val isRequired: Boolean,
	val showWhen: Map<String, Any>?,
	val repeat: Boolean,
	val parentQuestionId: UUID?,
	val displayOrder: Int,
)

data class DocTemplateDto(
	val id: UUID,
	val type: String,
	val displayName: String,
	val classification: String,
	val ownerSegmentId: UUID?,
	val isRequired: Boolean,
	val isConditional: Boolean,
	val condition: Map<String, Any>?,
	val guide: String?,
)

data class ActiveRulesResponse(
	val segments: List<SegmentDto>,
	val questions: List<QuestionDto>,
	val docTemplates: List<DocTemplateDto>,
)

@Tag(name = "Rules", description = "룰 조회 API")
@RestController
@RequestMapping("/rules")
class RuleController(
	private val service: RuleUseCase,
) {
	@Operation(summary = "C13 활성 룰 조회")
	@GetMapping("/active")
	fun getActive(
		@RequestParam(required = false) segment: String?,
	): ActiveRulesResponse {
		if (AuthContext.customer == null && AuthContext.staff == null) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다")
		}
		val rules = service.getActiveRules(segment)
		return ActiveRulesResponse(
			segments = rules.segments.map { it.toDto() },
			questions = rules.questions.map { it.toDto() },
			docTemplates = rules.docTemplates.map { it.toDto() },
		)
	}

	private fun Segment.toDto() = SegmentDto(id, axis, code, label)

	private fun Question.toDto() =
		QuestionDto(
			id = id,
			code = code,
			phase = phase,
			classification = classification,
			ownerSegmentId = ownerSegmentId,
			label = label,
			inputType = inputType,
			options = options,
			isRequired = isRequired,
			showWhen = showWhen,
			repeat = repeat,
			parentQuestionId = parentQuestionId,
			displayOrder = displayOrder,
		)

	private fun DocTemplate.toDto() =
		DocTemplateDto(
			id = id,
			type = type,
			displayName = displayName,
			classification = classification,
			ownerSegmentId = ownerSegmentId,
			isRequired = isRequired,
			isConditional = isConditional,
			condition = condition,
			guide = guide,
		)
}
