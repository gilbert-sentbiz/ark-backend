package com.sentbe.bizplatform.arc.rule.application.port.out

import com.sentbe.bizplatform.arc.rule.application.domain.ActiveRules

interface RulePort {
    fun getActiveRules(segmentCode: String?): ActiveRules
}
