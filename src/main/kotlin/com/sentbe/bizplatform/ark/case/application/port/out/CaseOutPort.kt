package com.sentbe.bizplatform.ark.case.application.port.out

import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import java.util.UUID

interface CaseOutPort {
	fun save(case: OnboardingCase): OnboardingCase

	fun findById(id: UUID): OnboardingCase?

	fun findByCustomerId(customerId: UUID): OnboardingCase?

	fun findAllForDashboard(): List<OnboardingCase>

	fun findCaseEvents(caseId: UUID): List<Map<String, Any>>

	fun createDocumentsForCase(
		caseId: UUID,
		docTemplates: List<Pair<UUID, Map<String, Any>>>,
	)

	fun countOpenRevisionsByCaseId(caseId: UUID): Int
}
