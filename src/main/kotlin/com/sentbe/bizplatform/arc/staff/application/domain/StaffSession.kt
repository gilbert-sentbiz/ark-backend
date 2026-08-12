package com.sentbe.bizplatform.arc.staff.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("staff_session")
data class StaffSession(
    @Id val id: UUID? = null,
    val staffId: UUID,
    val token: String,
    val expiresAt: OffsetDateTime,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
