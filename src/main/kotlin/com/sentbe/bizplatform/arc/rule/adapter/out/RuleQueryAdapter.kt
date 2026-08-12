package com.sentbe.bizplatform.arc.rule.adapter.out

import com.sentbe.bizplatform.arc.rule.application.domain.DocTemplate
import com.sentbe.bizplatform.arc.rule.application.domain.Question
import com.sentbe.bizplatform.arc.rule.application.domain.Segment
import com.sentbe.bizplatform.arc.rule.application.port.out.RuleQueryPort
import org.springframework.stereotype.Component

@Component
class RuleQueryAdapter(
    private val segmentRepo: SegmentRepository,
    private val questionRepo: QuestionRepository,
    private val docTemplateRepo: DocTemplateRepository,
) : RuleQueryPort {
    override fun findActiveSegments(): List<Segment> = segmentRepo.findAllActive()

    override fun findActiveQuestions(): List<Question> = questionRepo.findAllActive()

    override fun findActiveDocTemplates(): List<DocTemplate> = docTemplateRepo.findAllActive()
}
