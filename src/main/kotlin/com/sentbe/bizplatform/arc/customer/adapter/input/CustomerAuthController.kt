package com.sentbe.bizplatform.arc.customer.adapter.input

import com.sentbe.bizplatform.arc.customer.application.port.input.CustomerAuthUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class OtpRequestBody(
	val email: String,
)

data class OtpVerifyBody(
	val email: String,
	val code: String,
)

data class OtpSentResponse(
	val sent: Boolean = true,
)

data class TokenResponse(
	val token: String,
)

@Tag(name = "CustomerAuth", description = "고객 인증 API")
@RestController
@RequestMapping("/auth")
class CustomerAuthController(
	private val useCase: CustomerAuthUseCase,
) {
	@Operation(summary = "C11 OTP 코드 발급")
	@PostMapping("/otp/request")
	fun requestOtp(
		@RequestBody body: OtpRequestBody,
	): OtpSentResponse {
		useCase.requestOtp(body.email)
		return OtpSentResponse()
	}

	@Operation(summary = "C12 OTP 검증 + 세션 발급")
	@PostMapping("/otp/verify")
	fun verifyOtp(
		@RequestBody body: OtpVerifyBody,
	): TokenResponse = TokenResponse(useCase.verifyOtp(body.email, body.code))
}
