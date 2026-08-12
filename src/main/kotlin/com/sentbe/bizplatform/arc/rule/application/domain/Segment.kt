package com.sentbe.bizplatform.arc.rule.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("segment")
data class Segment(
    @Id val id: UUID,
    val axis: String,
    val code: String,
    val label: String,
    val classificationTrigger: List<Any>? = null,
    val questionOverrides: List<Any>? = null,
    val docOverrides: List<Any>? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val deactivatedAt: OffsetDateTime? = null,
)
