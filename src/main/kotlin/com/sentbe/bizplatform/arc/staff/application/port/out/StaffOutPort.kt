package com.sentbe.bizplatform.arc.staff.application.port.out

import com.sentbe.bizplatform.arc.staff.application.domain.Staff
import com.sentbe.bizplatform.arc.staff.application.domain.StaffSession

interface StaffOutPort {
    fun findByEmailAndIsActive(
        email: String,
        isActive: Boolean,
    ): Staff?

    fun saveSession(session: StaffSession)
}
