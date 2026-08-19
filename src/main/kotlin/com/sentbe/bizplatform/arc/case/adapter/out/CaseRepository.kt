package com.sentbe.bizplatform.arc.case.adapter.out

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface OnboardingCaseRepository : CrudRepository<OnboardingCaseJdbcEntity, UUID> {
	fun findTopByCustomerIdOrderByCreatedAtDesc(customerId: UUID): OnboardingCaseJdbcEntity?

	@Query("SELECT * FROM onboarding_case ORDER BY updated_at DESC")
	fun findAllForDashboard(): List<OnboardingCaseJdbcEntity>
}

interface CaseEventRepository : CrudRepository<CaseEventJdbcEntity, UUID> {
	fun findByCaseIdOrderByCreatedAt(caseId: UUID): List<CaseEventJdbcEntity>
}
