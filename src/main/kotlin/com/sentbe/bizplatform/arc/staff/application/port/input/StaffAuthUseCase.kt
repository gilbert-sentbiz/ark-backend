package com.sentbe.bizplatform.arc.staff.application.port.input

interface StaffAuthUseCase {
    fun mockLogin(email: String): String
}
