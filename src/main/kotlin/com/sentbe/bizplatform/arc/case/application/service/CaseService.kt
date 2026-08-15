package com.sentbe.bizplatform.arc.case.application.service

import com.sentbe.bizplatform.arc.case.application.domain.CaseStatus
import com.sentbe.bizplatform.arc.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.arc.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.arc.case.application.port.input.CaseUseCase
import com.sentbe.bizplatform.arc.case.application.port.out.CaseOutPort
import com.sentbe.bizplatform.arc.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.arc.global.event.Actor
import com.sentbe.bizplatform.arc.global.event.ActorType
import com.sentbe.bizplatform.arc.global.event.CaseEventAppender
import com.sentbe.bizplatform.arc.global.event.EventType
import com.sentbe.bizplatform.arc.rule.application.port.out.RulePort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CaseService(
    private val adapter: CaseOutPort,
    private val rulePort: RulePort,
    private val classificationService: ClassificationService,
    private val eventAppender: CaseEventAppender,
) : CaseUseCase {
    @Transactional
    override fun createCase(customerId: UUID): OnboardingCase {
        val existing = adapter.findByCustomerId(customerId)
        if (existing != null && existing.status !in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 케이스가 이미 있습니다")
        }

        val allRules = rulePort.getActiveRules(null)
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
    override fun saveIntake(
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
    override fun submitFirstIntake(
        caseId: UUID,
        customerId: UUID,
        answers: Map<String, Any>,
    ): OnboardingCase {
        val case = requireCase(caseId)
        requireCustomerOwns(case, customerId)
        if (case.status != CaseStatus.INQUIRY_RECEIVED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "1차 인테이크를 제출할 수 없는 상태입니다")
        }
        val existingIntake = adapter.findIntake(caseId, "first")
        if (existingIntake?.status == "submitted") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "1차 인테이크가 이미 제출되었습니다")
        }

        val intake =
            (existingIntake ?: IntakeResponse(caseId = caseId, phase = "first"))
                .copy(answers = answers, status = "submitted", submittedAt = java.time.OffsetDateTime.now())
        adapter.saveIntake(intake)

        val allRules = rulePort.getActiveRules(null)
        val matchedSegments = classificationService.classify(answers, allRules.segments)

        val secondQuestionIds =
            matchedSegments
                .flatMap { seg ->
                    rulePort
                        .getActiveRules(seg.code)
                        .questions
                        .filter { it.phase == "second" }
                        .map { it.id.toString() }
                }.distinct()

        val segmentMeta = mapOf("matchedSegments" to matchedSegments.map { mapOf("code" to it.code, "label" to it.label) })
        val services = matchedSegments.filter { it.axis == "service" }.map { it.code }
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
    override fun submitSecondIntake(
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
    override fun advanceStatus(
        caseId: UUID,
        staff: AuthenticatedStaff,
    ): OnboardingCase {
        val case = requireCase(caseId)
        val (nextStatus, requiredRole) =
            when (case.status) {
                CaseStatus.DOCUMENT_SUBMISSION_REQUIRED -> Pair(CaseStatus.INITIAL_SCREENING, "SALES")
                CaseStatus.INITIAL_SCREENING -> Pair(CaseStatus.DOCUMENT_SCREENING_REQUIRED, "SALES")
                CaseStatus.DOCUMENT_SCREENING_REQUIRED -> Pair(CaseStatus.APPROVAL_REVIEW_REQUIRED, "OPS")
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
    override fun closeCase(
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

    override fun getCase(caseId: UUID): OnboardingCase =
        adapter.findById(caseId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "케이스를 찾을 수 없습니다")

    override fun getCaseTimeline(caseId: UUID): List<Map<String, Any>> {
        requireCase(caseId)
        return adapter.findCaseEvents(caseId)
    }

    override fun getAllCases(): List<OnboardingCase> = adapter.findAllForDashboard()

    override fun getIntake(
        caseId: UUID,
        phase: String,
    ): IntakeResponse? = adapter.findIntake(caseId, phase)

    @Transactional
    override fun resubmit(
        caseId: UUID,
        customerId: UUID,
    ): OnboardingCase {
        val case = requireCase(caseId)
        requireCustomerOwns(case, customerId)
        if (case.status != CaseStatus.REVISION_REQUESTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "보완 요청 중인 케이스가 아닙니다")
        }
        if (adapter.countOpenRevisionsByCaseId(caseId) > 0) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "미해결 보완 요청이 남아 있습니다")
        }
        val returnTo =
            case.revisionRequestedFrom
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "복귀 상태 정보가 없습니다")

        val updated = case.copy(status = returnTo, revisionRequestedFrom = null)
        val saved = adapter.save(updated)
        eventAppender.append(
            caseId = caseId,
            eventType = EventType.CASE_STATUS_CHANGED,
            actor = Actor(ActorType.CUSTOMER, customerId),
            payload = mapOf("from" to CaseStatus.REVISION_REQUESTED, "to" to returnTo, "event" to "resubmitted"),
        )
        return saved
    }

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
                rulePort.getActiveRules(null).docTemplates
            } else {
                segmentCodes
                    .flatMap { code ->
                        rulePort.getActiveRules(code).docTemplates
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
