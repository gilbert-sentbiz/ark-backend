package com.sentbe.bizplatform.arc.case.application.port.input

import com.sentbe.bizplatform.arc.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.arc.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import java.util.UUID

interface CaseUseCase {
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
