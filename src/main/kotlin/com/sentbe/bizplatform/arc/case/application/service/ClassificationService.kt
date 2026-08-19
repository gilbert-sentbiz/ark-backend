package com.sentbe.bizplatform.arc.case.application.service

import com.sentbe.bizplatform.arc.rule.application.domain.Segment
import org.springframework.stereotype.Component

@Component
class ClassificationService {
	fun classify(
		answers: Map<String, Any>,
		segments: List<Segment>,
	): List<Segment> =
		segments
			.filter { seg -> seg.classificationTrigger?.any { rule -> evaluateRule(rule, answers) } == true }
			.sortedBy { seg ->
				seg.classificationTrigger
					?.filterIsInstance<Map<*, *>>()
					?.minOfOrNull { (it["priority"] as? Number)?.toInt() ?: Int.MAX_VALUE }
					?: Int.MAX_VALUE
			}

	@Suppress("UNCHECKED_CAST")
	private fun evaluateRule(
		rule: Any,
		answers: Map<String, Any>,
	): Boolean {
		if (rule !is Map<*, *>) return false
		val conditions = rule["conditions"] as? List<*> ?: return false
		val logic = rule["logic"] as? String ?: "AND"
		val results = conditions.map { cond -> evaluateCondition(cond, answers) }
		return if (logic == "OR") results.any { it } else results.all { it }
	}

	private fun evaluateCondition(
		condition: Any?,
		answers: Map<String, Any>,
	): Boolean {
		if (condition !is Map<*, *>) return false
		val field = condition["field"] as? String ?: return false
		val op = condition["op"] as? String ?: return false
		val expected = condition["value"] ?: return false
		val actual = answers[field] ?: return false
		return when (op) {
			"eq" -> {
				actual.toString() == expected.toString()
			}

			"contains" -> {
				when (actual) {
					is List<*> -> actual.any { it?.toString() == expected.toString() }
					else -> actual.toString().contains(expected.toString())
				}
			}

			"in" -> {
				val values = expected as? List<*> ?: listOf(expected)
				values.any { it?.toString() == actual.toString() }
			}

			else -> {
				false
			}
		}
	}
}
