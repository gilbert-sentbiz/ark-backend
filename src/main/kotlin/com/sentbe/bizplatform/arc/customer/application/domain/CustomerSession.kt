package com.sentbe.bizplatform.arc.customer.application.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("customer_session")
data class CustomerSession(
    @Id val id: UUID = UUID.randomUUID(),
    val customerId: UUID,
    val token: String,
    val expiresAt: OffsetDateTime,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
