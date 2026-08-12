package com.sentbe.bizplatform.arc.global.auth

import java.util.UUID

data class AuthenticatedCustomer(
    val id: UUID,
    val email: String,
)

data class AuthenticatedStaff(
    val id: UUID,
    val email: String,
    val role: String,
)

object AuthContext {
    private val customerHolder = ThreadLocal<AuthenticatedCustomer?>()
    private val staffHolder = ThreadLocal<AuthenticatedStaff?>()

    var customer: AuthenticatedCustomer?
        get() = customerHolder.get()
        set(value) = customerHolder.set(value)

    var staff: AuthenticatedStaff?
        get() = staffHolder.get()
        set(value) = staffHolder.set(value)

    fun clear() {
        customerHolder.remove()
        staffHolder.remove()
    }
}
