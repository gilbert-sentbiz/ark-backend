package com.sentbe.bizplatform.ark.staff.adapter.`in`

import com.sentbe.bizplatform.ark.staff.application.port.`in`.StaffAuthPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Internal Auth", description = "내부 직원 인증 API (local only)")
@RestController
@RequestMapping("/internal/auth")
@Profile("local")
class StaffAuthAdapter(
	private val useCase: StaffAuthPort,
) {
	@Operation(summary = "I7 내부 SSO 목 로그인 (local)")
	@PostMapping("/mock-login")
	fun mockLogin(
		@RequestBody body: MockLoginRequest,
	): StaffTokenResponse = StaffTokenResponse(useCase.mockLogin(body.email))
}
