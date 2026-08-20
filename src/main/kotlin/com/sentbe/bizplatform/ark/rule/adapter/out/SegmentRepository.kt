package com.sentbe.bizplatform.ark.rule.adapter.out

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface SegmentRepository : CrudRepository<SegmentJdbcEntity, UUID> {
	@Query("SELECT * FROM segment WHERE deactivated_at IS NULL ORDER BY axis, code")
	fun findAllActive(): List<SegmentJdbcEntity>
}
