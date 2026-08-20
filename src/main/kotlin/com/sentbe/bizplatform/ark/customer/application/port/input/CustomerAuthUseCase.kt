package com.sentbe.bizplatform.ark.customer.application.port.input

import java.util.UUID

interface CustomerAuthUseCase {
	fun requestOtp(email: String)

	fun verifyOtp(
		email: String,
		code: String,
	): String
}
