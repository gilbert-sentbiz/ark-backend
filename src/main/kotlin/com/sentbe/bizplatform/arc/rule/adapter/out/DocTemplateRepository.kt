package com.sentbe.bizplatform.arc.rule.adapter.out

import com.sentbe.bizplatform.arc.rule.application.domain.DocTemplate
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface DocTemplateRepository : CrudRepository<DocTemplate, UUID> {
    @Query("SELECT * FROM doc_template WHERE deactivated_at IS NULL ORDER BY type")
    fun findAllActive(): List<DocTemplate>
}
