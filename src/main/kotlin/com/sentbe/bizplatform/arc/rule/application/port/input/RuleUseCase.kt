package com.sentbe.bizplatform.arc.rule.application.port.input

import com.sentbe.bizplatform.arc.rule.application.domain.ActiveRules

interface RuleUseCase {
	fun getActiveRules(segmentCode: String?): ActiveRules
}
