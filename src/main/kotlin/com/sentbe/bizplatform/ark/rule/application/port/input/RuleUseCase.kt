package com.sentbe.bizplatform.ark.rule.application.port.input

import com.sentbe.bizplatform.ark.rule.application.domain.ActiveRules

interface RuleUseCase {
	fun getActiveRules(segmentCode: String?): ActiveRules
}
