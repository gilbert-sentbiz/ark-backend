package com.sentbe.bizplatform.ark.intake.adapter.out

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface IntakeResponseRepository : CrudRepository<IntakeResponseJdbcEntity, UUID> {
	fun findByCaseIdAndPhase(
		caseId: UUID,
		phase: String,
	): IntakeResponseJdbcEntity?
}
