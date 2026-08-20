package com.sentbe.bizplatform.ark.rule.adapter.out

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface DocTemplateRepository : CrudRepository<DocTemplateJdbcEntity, UUID> {
	@Query("SELECT * FROM doc_template WHERE deactivated_at IS NULL ORDER BY type")
	fun findAllActive(): List<DocTemplateJdbcEntity>
}
