package com.sentbe.bizplatform.arc.case.adapter.input

import com.sentbe.bizplatform.arc.case.application.service.CaseService
import com.sentbe.bizplatform.arc.global.auth.AuthContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class RevisionBody(
    val reason: String,
)

data class CloseBody(
    val reason: String,
)

@RestController
@RequestMapping("/internal/cases")
class InternalCaseController(
    private val service: CaseService,
) {
    @GetMapping
    fun getCases(): List<Map<String, Any>> {
        requireStaff()
        return service.getAllCases().map { case ->
            mapOf(
                "id" to case.id,
                "customerId" to case.customerId,
                "status" to case.status,
                "assigneeStaffId" to (case.assigneeStaffId ?: ""),
                "updatedAt" to case.updatedAt,
                "createdAt" to case.createdAt,
            )
        }
    }

    @GetMapping("/{id}")
    fun getCase(
        @PathVariable id: UUID,
    ): Map<String, Any> {
        requireStaff()
        val case = service.getCase(id)
        val timeline = service.getCaseTimeline(id)
        return mapOf(
            "id" to case.id,
            "customerId" to case.customerId,
            "status" to case.status,
            "entityCode" to (case.entityCode ?: ""),
            "services" to case.services,
            "segmentMeta" to case.segmentMeta,
            "pinnedQuestionIds" to case.pinnedQuestionIds,
            "assigneeStaffId" to (case.assigneeStaffId ?: ""),
            "closeReason" to (case.closeReason ?: ""),
            "revisionRequestedFrom" to (case.revisionRequestedFrom ?: ""),
            "createdAt" to case.createdAt,
            "updatedAt" to case.updatedAt,
            "timeline" to timeline,
        )
    }

    @PostMapping("/{id}/advance")
    fun advance(
        @PathVariable id: UUID,
    ): Map<String, Any> {
        val staff = requireStaff()
        val case = service.advanceStatus(id, staff)
        return mapOf("id" to case.id, "status" to case.status)
    }

    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
        @RequestBody body: RevisionBody,
    ): Map<String, Any> {
        val staff = requireStaff()
        val case = service.requestRevision(id, staff, body.reason)
        return mapOf("id" to case.id, "status" to case.status)
    }

    @PostMapping("/{id}/close")
    fun close(
        @PathVariable id: UUID,
        @RequestBody body: CloseBody,
    ): Map<String, Any> {
        val staff = requireStaff()
        val case = service.closeCase(id, staff, body.reason)
        return mapOf("id" to case.id, "status" to case.status, "closeReason" to (case.closeReason ?: ""))
    }

    private fun requireStaff() =
        AuthContext.staff
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "직원 인증이 필요합니다")
}
