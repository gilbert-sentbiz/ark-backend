package com.sentbe.bizplatform.ark.rule.application.port.`in`

import com.sentbe.bizplatform.ark.rule.application.domain.ActiveRules

interface RulePort {
	fun getActiveRules(segmentCode: String?): ActiveRules
}
