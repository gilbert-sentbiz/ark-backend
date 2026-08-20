package com.sentbe.bizplatform.ark.staff.application.port.input

interface StaffAuthUseCase {
	fun mockLogin(email: String): String
}
