package com.sentbe.bizplatform.arc.staff.adapter.`in`

import com.sentbe.bizplatform.arc.staff.application.port.`in`.StaffAuthUseCase
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class MockLoginRequest(
    val email: String,
)

data class StaffTokenResponse(
    val token: String,
)

@RestController
@RequestMapping("/internal/auth")
@Profile("local")
class StaffAuthController(
    private val useCase: StaffAuthUseCase,
) {
    @PostMapping("/mock-login")
    fun mockLogin(
        @RequestBody body: MockLoginRequest,
    ): StaffTokenResponse = StaffTokenResponse(useCase.mockLogin(body.email))
}
