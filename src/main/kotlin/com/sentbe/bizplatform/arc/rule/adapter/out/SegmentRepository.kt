package com.sentbe.bizplatform.arc.rule.adapter.out

import com.sentbe.bizplatform.arc.rule.application.domain.Segment
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface SegmentRepository : CrudRepository<Segment, UUID> {
    @Query("SELECT * FROM segment WHERE deactivated_at IS NULL ORDER BY axis, code")
    fun findAllActive(): List<Segment>
}
