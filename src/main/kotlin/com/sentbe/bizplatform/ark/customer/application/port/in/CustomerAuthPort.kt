package com.sentbe.bizplatform.ark.customer.application.port.`in`

import java.util.UUID

interface CustomerAuthPort {
	fun requestOtp(email: String)

	fun verifyOtp(
		email: String,
		code: String,
	): String
}
