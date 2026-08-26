package com.sentbe.bizplatform.ark.case.application.port.`in`

import com.sentbe.bizplatform.ark.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedStaff
import java.util.UUID

interface CasePort {
	fun createCase(customerId: UUID): OnboardingCase

	fun saveIntake(
		caseId: UUID,
		customerId: UUID,
		phase: String,
		answers: Map<String, Any>,
	): IntakeResponse

	fun submitFirstIntake(
		caseId: UUID,
		customerId: UUID,
		answers: Map<String, Any>,
	): OnboardingCase

	fun submitSecondIntake(
		caseId: UUID,
		customerId: UUID,
		answers: Map<String, Any>,
	): OnboardingCase

	fun advanceStatus(
		caseId: UUID,
		staff: AuthenticatedStaff,
	): OnboardingCase

	fun closeCase(
		caseId: UUID,
		staff: AuthenticatedStaff,
		reason: String,
	): OnboardingCase

	fun getCase(caseId: UUID): OnboardingCase

	// PI-242: 고객 본인 케이스 조회(id 없이). 재로그인/타 기기에서 케이스 복귀용.
	fun findMyCase(customerId: UUID): OnboardingCase?

	fun getCaseTimeline(caseId: UUID): List<Map<String, Any>>

	fun getAllCases(): List<OnboardingCase>

	fun getIntake(
		caseId: UUID,
		phase: String,
	): IntakeResponse?

	fun resubmit(
		caseId: UUID,
		customerId: UUID,
	): OnboardingCase
}
