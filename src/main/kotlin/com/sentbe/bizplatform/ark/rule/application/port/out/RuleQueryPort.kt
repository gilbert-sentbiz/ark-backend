package com.sentbe.bizplatform.ark.rule.application.port.out

import com.sentbe.bizplatform.ark.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.ark.rule.application.domain.Question
import com.sentbe.bizplatform.ark.rule.application.domain.Segment

interface RuleQueryPort {
	fun findActiveSegments(): List<Segment>

	fun findActiveQuestions(): List<Question>

	fun findActiveDocTemplates(): List<DocTemplate>
}
