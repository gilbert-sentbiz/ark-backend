package com.sentbe.bizplatform.ark.rule.application.domain

data class ActiveRules(
	val segments: List<Segment>,
	val questions: List<Question>,
	val docTemplates: List<DocTemplate>,
)
