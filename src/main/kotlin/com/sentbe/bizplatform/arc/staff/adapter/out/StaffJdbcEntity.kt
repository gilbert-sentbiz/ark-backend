package com.sentbe.bizplatform.arc.staff.adapter.out

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("staff")
data class StaffJdbcEntity(
	@Id @Column("id") val id: UUID,
	@Column("email") val email: String,
	@Column("name") val name: String,
	@Column("role") val role: String,
	@Column("is_active") val isActive: Boolean = true,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: OffsetDateTime? = null,
)

@Table("staff_session")
data class StaffSessionJdbcEntity(
	@Id @Column("id") val id: UUID? = null,
	@Column("staff_id") val staffId: UUID,
	@Column("token") val token: String,
	@Column("expires_at") val expiresAt: OffsetDateTime,
	@CreatedDate @Column("created_at") @ReadOnlyProperty val createdAt: OffsetDateTime? = null,
)
