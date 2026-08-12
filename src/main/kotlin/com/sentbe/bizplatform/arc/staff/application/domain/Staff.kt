package com.sentbe.bizplatform.arc.staff.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("staff")
data class Staff(
    @Id val id: UUID = UUID.randomUUID(),
    val email: String,
    val name: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
