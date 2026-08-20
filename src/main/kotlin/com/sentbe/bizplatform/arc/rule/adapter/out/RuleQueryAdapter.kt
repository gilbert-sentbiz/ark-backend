package com.sentbe.bizplatform.arc.rule.adapter.out

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.sentbe.bizplatform.arc.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.arc.rule.application.domain.Question
import com.sentbe.bizplatform.arc.rule.application.domain.Segment
import com.sentbe.bizplatform.arc.rule.application.port.out.RuleQueryPort
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

private val MAPPER = ObjectMapper().findAndRegisterModules()
private val LIST_ANY = object : TypeReference<List<Any>>() {}
private val MAP_ANY = object : TypeReference<Map<String, Any>>() {}

@Component
class RuleQueryAdapter(
	private val segmentRepo: SegmentRepository,
	private val questionRepo: QuestionRepository,
	private val docTemplateRepo: DocTemplateRepository,
) : RuleQueryPort {
	override fun findActiveSegments(): List<Segment> = segmentRepo.findAllActive().map { it.toDomain() }

	override fun findActiveQuestions(): List<Question> = questionRepo.findAllActive().map { it.toDomain() }

	override fun findActiveDocTemplates(): List<DocTemplate> = docTemplateRepo.findAllActive().map { it.toDomain() }

	private fun SegmentJdbcEntity.toDomain() =
		Segment(
			id = id,
			axis = axis,
			code = code,
			label = label,
			classificationTrigger = classificationTrigger?.parseList(),
			questionOverrides = questionOverrides?.parseList(),
			docOverrides = docOverrides?.parseList(),
			createdAt = createdAt ?: OffsetDateTime.now(),
			deactivatedAt = deactivatedAt,
		)

	private fun QuestionJdbcEntity.toDomain() =
		Question(
			id = id,
			code = code,
			phase = phase,
			classification = classification,
			ownerSegmentId = ownerSegmentId,
			label = label,
			inputType = inputType,
			options = options?.parseList(),
			isRequired = isRequired,
			showWhen = showWhen?.parseMap(),
			repeat = repeat,
			parentQuestionId = parentQuestionId,
			displayOrder = displayOrder,
			replacesQuestionId = replacesQuestionId,
			createdByStaffId = createdByStaffId,
			createdAt = createdAt ?: OffsetDateTime.now(),
			deactivatedAt = deactivatedAt,
		)

	private fun DocTemplateJdbcEntity.toDomain() =
		DocTemplate(
			id = id,
			type = type,
			displayName = displayName,
			classification = classification,
			ownerSegmentId = ownerSegmentId,
			isRequired = isRequired,
			isConditional = isConditional,
			condition = condition?.parseMap(),
			guide = guide,
			createdAt = createdAt ?: OffsetDateTime.now(),
			deactivatedAt = deactivatedAt,
		)

	private fun String.parseList(): List<Any> = MAPPER.readValue(this, LIST_ANY)

	private fun String.parseMap(): Map<String, Any> = MAPPER.readValue(this, MAP_ANY)
}
