package com.sentbe.bizplatform.ark.rule.application.service

import com.sentbe.bizplatform.ark.rule.application.domain.ActiveRules
import com.sentbe.bizplatform.ark.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.ark.rule.application.domain.Question
import com.sentbe.bizplatform.ark.rule.application.domain.Segment
import com.sentbe.bizplatform.ark.rule.application.port.input.RuleUseCase
import com.sentbe.bizplatform.ark.rule.application.port.out.RulePort
import com.sentbe.bizplatform.ark.rule.application.port.out.RuleQueryPort
import org.springframework.stereotype.Service

@Service
class RuleQueryService(
	private val port: RuleQueryPort,
) : RulePort,
	RuleUseCase {
	override fun getActiveRules(segmentCode: String?): ActiveRules {
		val segments = port.findActiveSegments()
		val allQuestions = port.findActiveQuestions()
		val allDocs = port.findActiveDocTemplates()

		if (segmentCode == null) {
			return ActiveRules(
				segments = segments,
				questions = allQuestions.filter { it.classification == "common" },
				docTemplates = allDocs.filter { it.classification == "common" },
			)
		}

		val segment =
			segments.find { it.code == segmentCode }
				?: throw NoSuchElementException("세그먼트를 찾을 수 없습니다: $segmentCode")

		val questions = buildQuestions(segment, allQuestions)
		val docs = buildDocTemplates(segment, allDocs)
		return ActiveRules(segments = segments, questions = questions, docTemplates = docs)
	}

	private fun buildQuestions(
		segment: Segment,
		all: List<Question>,
	): List<Question> {
		val base =
			all.filter { q ->
				q.classification == "common" || (q.classification == "own" && q.ownerSegmentId == segment.id)
			}

		val overrides = segment.questionOverrides ?: return base

		val overrideMap =
			overrides
				.filterIsInstance<Map<*, *>>()
				.associateBy { it["code"] as? String }

		return base.mapNotNull { q ->
			val ov = overrideMap[q.code] ?: return@mapNotNull q
			val enabled = ov["enabled"] as? Boolean ?: true
			if (!enabled) return@mapNotNull null
			@Suppress("UNCHECKED_CAST")
			val filteredOptions = ov["filterOptions"] as? List<Any>
			if (filteredOptions != null) q.copy(options = filteredOptions) else q
		}
	}

	private fun buildDocTemplates(
		segment: Segment,
		all: List<DocTemplate>,
	): List<DocTemplate> {
		val base =
			all.filter { d ->
				d.classification == "common" || (d.classification == "own" && d.ownerSegmentId == segment.id)
			}

		val overrides = segment.docOverrides ?: return dedupByType(base)

		val overrideMap =
			overrides
				.filterIsInstance<Map<*, *>>()
				.associateBy { it["type"] as? String }

		val result =
			base.mapNotNull { d ->
				val ov = overrideMap[d.type] ?: return@mapNotNull d
				val enabled = ov["enabled"] as? Boolean ?: true
				if (!enabled) null else d
			}
		return dedupByType(result)
	}

	private fun dedupByType(docs: List<DocTemplate>): List<DocTemplate> = docs.distinctBy { it.type }
}
