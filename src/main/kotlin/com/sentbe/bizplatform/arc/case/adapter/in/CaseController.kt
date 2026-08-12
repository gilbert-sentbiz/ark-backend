package com.sentbe.bizplatform.arc.case.adapter.`in`

import com.sentbe.bizplatform.arc.case.application.port.`in`.CaseUseCase
import com.sentbe.bizplatform.arc.global.auth.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class IntakeBody(
    val answers: Map<String, Any>,
)

@RestController
@RequestMapping("/cases")
class CaseController(
    private val service: CaseUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCase(): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val case = service.createCase(customer.id)
        return mapOf("id" to case.id, "status" to case.status)
    }

    @GetMapping("/{id}")
    fun getCase(
        @PathVariable id: UUID,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val case = service.getCase(id)
        if (case.customerId != customer.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다")
        }
        return caseToMap(case)
    }

    @PutMapping("/{id}/intake/first")
    fun saveFirstIntake(
        @PathVariable id: UUID,
        @RequestBody body: IntakeBody,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val intake = service.saveIntake(id, customer.id, "first", body.answers)
        return mapOf("phase" to intake.phase, "status" to intake.status)
    }

    @PostMapping("/{id}/intake/first/submit")
    fun submitFirstIntake(
        @PathVariable id: UUID,
        @RequestBody body: IntakeBody,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val case = service.submitFirstIntake(id, customer.id, body.answers)
        return caseToMap(case)
    }

    @PutMapping("/{id}/intake/second")
    fun saveSecondIntake(
        @PathVariable id: UUID,
        @RequestBody body: IntakeBody,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val intake = service.saveIntake(id, customer.id, "second", body.answers)
        return mapOf("phase" to intake.phase, "status" to intake.status)
    }

    @PostMapping("/{id}/intake/second/submit")
    fun submitSecondIntake(
        @PathVariable id: UUID,
        @RequestBody body: IntakeBody,
    ): Map<String, Any> {
        val customer =
            AuthContext.customer
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "고객 인증이 필요합니다")
        val case = service.submitSecondIntake(id, customer.id, body.answers)
        return caseToMap(case)
    }

    private fun caseToMap(case: com.sentbe.bizplatform.arc.case.application.domain.OnboardingCase): Map<String, Any> =
        mapOf(
            "id" to case.id,
            "status" to case.status,
            "pinnedQuestionIds" to case.pinnedQuestionIds,
            "segmentMeta" to case.segmentMeta,
            "entityCode" to (case.entityCode ?: ""),
            "services" to case.services,
        )
}
