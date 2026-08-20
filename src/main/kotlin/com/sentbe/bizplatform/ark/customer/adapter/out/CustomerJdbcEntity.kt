package com.sentbe.bizplatform.ark.customer.adapter.out

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Table("customer")
data class CustomerJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("email") val email: String,
	@Column("auth_method") val authMethod: String = "otp",
	@Column("password_hash") val passwordHash: String? = null,
	@Column("business_reg_no") val businessRegNo: String? = null,
	@Column("company_name") val companyName: String? = null,
	@Column("contact_name") val contactName: String? = null,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
)

@Table("customer_session")
data class CustomerSessionJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("customer_id") val customerId: UUID,
	@Column("token") val token: String,
	@Column("expires_at") val expiresAt: OffsetDateTime,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: Instant? = null,
)
