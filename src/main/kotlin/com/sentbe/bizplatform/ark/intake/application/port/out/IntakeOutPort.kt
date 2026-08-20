package com.sentbe.bizplatform.ark.intake.application.port.out

import com.sentbe.bizplatform.ark.case.application.domain.IntakeResponse
import java.util.UUID

interface IntakeOutPort {
	fun save(intake: IntakeResponse): IntakeResponse

	fun findByCaseIdAndPhase(
		caseId: UUID,
		phase: String,
	): IntakeResponse?
}
