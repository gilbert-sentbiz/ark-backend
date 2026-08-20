package com.sentbe.bizplatform.arc.rule.adapter.out

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface QuestionRepository : CrudRepository<QuestionJdbcEntity, UUID> {
	@Query("SELECT * FROM question WHERE deactivated_at IS NULL ORDER BY display_order, code")
	fun findAllActive(): List<QuestionJdbcEntity>
}
