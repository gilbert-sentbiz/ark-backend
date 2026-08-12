package com.sentbe.bizplatform.arc.customer.adapter.`in`

import com.sentbe.bizplatform.arc.customer.application.port.`in`.CustomerAuthUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class OtpRequestBody(
    val email: String,
)

data class OtpVerifyBody(
    val email: String,
    val code: String,
)

data class TokenResponse(
    val token: String,
)

@RestController
@RequestMapping("/auth")
class CustomerAuthController(
    private val useCase: CustomerAuthUseCase,
) {
    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun requestOtp(
        @RequestBody body: OtpRequestBody,
    ) {
        useCase.requestOtp(body.email)
    }

    @PostMapping("/otp/verify")
    fun verifyOtp(
        @RequestBody body: OtpVerifyBody,
    ): TokenResponse = TokenResponse(useCase.verifyOtp(body.email, body.code))
}
