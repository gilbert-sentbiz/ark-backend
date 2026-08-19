package com.sentbe.bizplatform.arc.global.health

import com.sentbe.bizplatform.arc.global.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
	@GetMapping("/health")
	fun health(): ResponseEntity<ApiResponse<Map<String, String>>> = ResponseEntity.ok(ApiResponse.okData(mapOf("status" to "ok")))
}
