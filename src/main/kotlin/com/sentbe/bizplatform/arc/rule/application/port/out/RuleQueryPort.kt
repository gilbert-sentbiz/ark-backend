package com.sentbe.bizplatform.arc.rule.application.port.out

import com.sentbe.bizplatform.arc.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.arc.rule.application.domain.Question
import com.sentbe.bizplatform.arc.rule.application.domain.Segment

interface RuleQueryPort {
    fun findActiveSegments(): List<Segment>

    fun findActiveQuestions(): List<Question>

    fun findActiveDocTemplates(): List<DocTemplate>
}
