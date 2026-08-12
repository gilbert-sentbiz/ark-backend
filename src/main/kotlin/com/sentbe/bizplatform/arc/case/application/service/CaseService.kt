package com.sentbe.bizplatform.arc.case.application.service

import com.sentbe.bizplatform.arc.case.adapter.out.CaseJdbcAdapter
import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.arc.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.arc.global.event.Actor
import com.sentbe.bizplatform.arc.global.event.ActorType
import com.sentbe.bizplatform.arc.global.event.CaseEventAppender
import com.sentbe.bizplatform.arc.global.event.EventType
import com.sentbe.bizplatform.arc.rule.application.service.RuleQueryService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CaseService(
    private val adapter: CaseJdbcAdapter,
    private val ruleService: RuleQueryService,
    private val classificationService: ClassificationService,
    private val eventAppender: CaseEventAppender,
) {
    @Transactional
    fun createCase(customerId: UUID): OnboardingCase {
        val existing = adapter.findByCustomerId(customerId)
        if (existing != null && existing.status !in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 케이스가 이미 있습니다")
        }

        val allRules = ruleService.getActiveRules(null)
        val firstQuestionIds =
            allRules.questions
                .filter { it.phase == "first" }
                .map { it.id.toString() }

        val caseId = UUID.randomUUID()
        val newCase =
            OnboardingCase(
                id = caseId,
                customerId = customerId,
                status = CaseStatus.INQUIRY_RECEIVED,
                pinnedQuestionIds = mapOf("first" to firstQuestionIds),
            )
        val saved = adapter.save(newCase)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_CREATED,
            actor = Actor(ActorType.CUSTOMER, customerId),
            payload = mapOf("status" to CaseStatus.INQUIRY_RECEIVED),
        )
        return saved
    }

    @Transactional
    fun saveIntake(
        caseId: UUID,
        customerId: UUID,
        phase: String,
        answers: Map<String, Any>,
    ): IntakeResponse {
        val case = requireCase(caseId)
        requireCustomerOwns(case, customerId)
        val intake =
            adapter.findIntake(caseId, phase)
                ?: IntakeResponse(caseId = caseId, phase = phase)
        val saved = adapter.saveIntake(intake.copy(answers = answers))
        adapter.save(case.copy(lastCustomerActionAt = saved.savedAt))
        return saved
    }

    @Transactional
    fun submitFirstIntake(
        caseId: UUID,
        customerId: UUID,
        answers: Map<String, Any>,
    ): OnboardingCase {
        val case = requireCase(caseId)
        requireCustomerOwns(case, customerId)
        if (case.status != CaseStatus.INQUIRY_RECEIVED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "1차 인테이크를 제출할 수 없는 상태입니다")
        }

        val intake =
            (adapter.findIntake(caseId, "first") ?: IntakeResponse(caseId = caseId, phase = "first"))
                .copy(answers = answers, status = "submitted", submittedAt = java.time.OffsetDateTime.now())
        adapter.saveIntake(intake)

        val allRules = ruleService.getActiveRules(null)
        val matchedSegments = classificationService.classify(answers, allRules.segments)

        val secondQuestionIds =
            matchedSegments
                .flatMap { seg ->
                    ruleService
                        .getActiveRules(seg.code)
                        .questions
                        .filter { it.phase == "second" }
                        .map { it.id.toString() }
                }.distinct()

        val segmentMeta = mapOf("matchedSegments" to matchedSegments.map { mapOf("code" to it.code, "label" to it.label) })
        val services =
            answers["services"]?.let { v ->
                when (v) {
                    is List<*> -> v.filterNotNull().map { it.toString() }
                    else -> listOf(v.toString())
                }
            } ?: emptyList()
        val entityCode = matchedSegments.firstOrNull { it.axis == "entity" }?.code

        val updated =
            case.copy(
                entityCode = entityCode,
                services = services,
                segmentMeta = segmentMeta,
                pinnedQuestionIds = case.pinnedQuestionIds + mapOf("second" to secondQuestionIds),
                lastCustomerActionAt = intake.submittedAt,
            )
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.CUSTOMER, customerId),
            payload = mapOf("event" to "first_intake_submitted", "matchedSegments" to matchedSegments.map { it.code }),
        )
        return saved
    }

    @Transactional
    fun submitSecondIntake(
        caseId: UUID,
        customerId: UUID,
        answers: Map<String, Any>,
    ): OnboardingCase {
        val case = requireCase(caseId)
        requireCustomerOwns(case, customerId)
        if (case.status != CaseStatus.INQUIRY_RECEIVED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "2차 인테이크를 제출할 수 없는 상태입니다")
        }
        val pinnedSecond = case.pinnedQuestionIds["second"]
        if (pinnedSecond == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "1차 인테이크를 먼저 제출해야 합니다")
        }

        val intake =
            (adapter.findIntake(caseId, "second") ?: IntakeResponse(caseId = caseId, phase = "second"))
                .copy(answers = answers, status = "submitted", submittedAt = java.time.OffsetDateTime.now())
        adapter.saveIntake(intake)

        createDocumentsForCase(case)

        val updated =
            case.copy(
                status = CaseStatus.DOCUMENT_SUBMISSION_REQUIRED,
                lastCustomerActionAt = intake.submittedAt,
            )
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.CUSTOMER, customerId),
            payload = mapOf("from" to CaseStatus.INQUIRY_RECEIVED, "to" to CaseStatus.DOCUMENT_SUBMISSION_REQUIRED),
        )
        return saved
    }

    @Transactional
    fun advanceStatus(
        caseId: UUID,
        staff: AuthenticatedStaff,
    ): OnboardingCase {
        val case = requireCase(caseId)
        val (nextStatus, requiredRole) =
            when (case.status) {
                CaseStatus.INQUIRY_RECEIVED -> Pair(CaseStatus.DOCUMENT_SUBMISSION_REQUIRED, "OPS")
                CaseStatus.DOCUMENT_SUBMISSION_REQUIRED -> Pair(CaseStatus.INITIAL_SCREENING, "OPS")
                CaseStatus.INITIAL_SCREENING -> Pair(CaseStatus.APPROVAL_REVIEW_REQUIRED, "COMPLIANCE")
                CaseStatus.APPROVAL_REVIEW_REQUIRED -> Pair(CaseStatus.ACCOUNT_SETUP_REQUIRED, "COMPLIANCE")
                CaseStatus.ACCOUNT_SETUP_REQUIRED -> Pair(CaseStatus.COMPLETED, "OPS")
                else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이 상태에서는 전진할 수 없습니다: ${case.status}")
            }
        requireRole(staff, requiredRole)

        val updated = case.copy(status = nextStatus, assigneeStaffId = staff.id)
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.STAFF, staff.id),
            payload = mapOf("from" to case.status, "to" to nextStatus),
        )
        return saved
    }

    @Transactional
    fun requestRevision(
        caseId: UUID,
        staff: AuthenticatedStaff,
        reason: String,
    ): OnboardingCase {
        val case = requireCase(caseId)
        if (case.status in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 케이스는 반려할 수 없습니다")
        }
        requireRole(staff, "OPS", "COMPLIANCE", "ADMIN")

        val updated =
            case.copy(
                status = CaseStatus.REVISION_REQUESTED,
                revisionRequestedFrom = case.status,
            )
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.STAFF, staff.id),
            payload = mapOf("from" to case.status, "to" to CaseStatus.REVISION_REQUESTED, "reason" to reason),
        )
        return saved
    }

    @Transactional
    fun closeCase(
        caseId: UUID,
        staff: AuthenticatedStaff,
        reason: String,
    ): OnboardingCase {
        val case = requireCase(caseId)
        if (case.status in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 종료된 케이스입니다")
        }
        if (reason !in setOf("DROPPED", "EXITED")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 사유는 DROPPED 또는 EXITED여야 합니다")
        }

        val updated = case.copy(status = CaseStatus.CLOSED, closeReason = reason)
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.STAFF, staff.id),
            payload = mapOf("to" to CaseStatus.CLOSED, "reason" to reason),
        )
        return saved
    }

    fun getCase(caseId: UUID): OnboardingCase =
        adapter.findById(caseId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "케이스를 찾을 수 없습니다")

    fun getCaseTimeline(caseId: UUID): List<Map<String, Any>> {
        requireCase(caseId)
        return adapter.findCaseEvents(caseId)
    }

    fun getAllCases(): List<OnboardingCase> = adapter.findAllForDashboard()

    private fun requireCase(caseId: UUID): OnboardingCase =
        adapter.findById(caseId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "케이스를 찾을 수 없습니다")

    private fun requireCustomerOwns(
        case: OnboardingCase,
        customerId: UUID,
    ) {
        if (case.customerId != customerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다")
        }
    }

    private fun requireRole(
        staff: AuthenticatedStaff,
        vararg roles: String,
    ) {
        if (staff.role !in roles) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "이 작업에 필요한 역할: ${roles.joinToString()}")
        }
    }

    private fun createDocumentsForCase(case: OnboardingCase) {
        val segmentCodes =
            (case.segmentMeta["matchedSegments"] as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.mapNotNull { it["code"] as? String }
                ?: emptyList()

        val docTemplates =
            if (segmentCodes.isEmpty()) {
                ruleService.getActiveRules(null).docTemplates
            } else {
                segmentCodes
                    .flatMap { code ->
                        ruleService.getActiveRules(code).docTemplates
                    }.distinctBy { it.type }
            }

        adapter.createDocumentsForCase(
            caseId = case.id,
            docTemplates =
                docTemplates.map { doc ->
                    doc.id to
                        mapOf(
                            "type" to doc.type,
                            "displayName" to doc.displayName,
                            "isRequired" to doc.isRequired,
                            "isConditional" to doc.isConditional,
                        )
                },
        )
    }
}
