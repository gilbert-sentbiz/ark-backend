package com.sentbe.bizplatform.ark.case.application.service

import com.sentbe.bizplatform.ark.case.application.domain.CaseStatus
import com.sentbe.bizplatform.ark.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.ark.case.application.port.`in`.CasePort
import com.sentbe.bizplatform.ark.case.application.port.out.CaseOutPort
import com.sentbe.bizplatform.ark.global.auth.AuthenticatedStaff
import com.sentbe.bizplatform.ark.global.event.Actor
import com.sentbe.bizplatform.ark.global.event.ActorType
import com.sentbe.bizplatform.ark.global.event.CaseEventAppender
import com.sentbe.bizplatform.ark.global.event.EventType
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import com.sentbe.bizplatform.ark.intake.application.port.out.IntakeOutPort
import com.sentbe.bizplatform.ark.rule.application.port.`in`.RulePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CaseService(
	private val adapter: CaseOutPort,
	private val intakePort: IntakeOutPort,
	private val rulePort: RulePort,
	private val classificationService: ClassificationService,
	private val eventAppender: CaseEventAppender,
) : CasePort {
	@Transactional
	override fun createCase(customerId: UUID): OnboardingCase {
		val existing = adapter.findByCustomerId(customerId)
		if (existing != null && existing.status !in setOf(CaseStatus.COMPLETED, CaseStatus.CLOSED)) {
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
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
			intakePort.findByCaseIdAndPhase(caseId, phase)
				?: IntakeResponse(caseId = caseId, phase = phase)
		val saved = intakePort.save(intake.copy(answers = answers))
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
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}
		val existingIntake = intakePort.findByCaseIdAndPhase(caseId, "first")
		if (existingIntake?.status == "submitted") {
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}

		val intake =
			(existingIntake ?: IntakeResponse(caseId = caseId, phase = "first"))
				.copy(answers = answers, status = "submitted", submittedAt = java.time.OffsetDateTime.now())
		intakePort.save(intake)

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
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}
		val pinnedSecond = case.pinnedQuestionIds["second"]
		if (pinnedSecond == null) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}

		val intake =
			(intakePort.findByCaseIdAndPhase(caseId, "second") ?: IntakeResponse(caseId = caseId, phase = "second"))
				.copy(answers = answers, status = "submitted", submittedAt = java.time.OffsetDateTime.now())
		intakePort.save(intake)

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
				else -> throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
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
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
		if (reason !in setOf("DROPPED", "EXITED")) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
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

	override fun getCase(caseId: UUID): OnboardingCase = adapter.findById(caseId) ?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)

	// PI-242: 고객 본인 케이스(id 없이). 1계정 1활성 케이스 정책상 최대 1건.
	override fun findMyCase(customerId: UUID): OnboardingCase? = adapter.findByCustomerId(customerId)

	override fun getCaseTimeline(caseId: UUID): List<Map<String, Any>> {
		requireCase(caseId)
		return adapter.findCaseEvents(caseId)
	}

	override fun getAllCases(): List<OnboardingCase> = adapter.findAllForDashboard()

	override fun getIntake(
		caseId: UUID,
		phase: String,
	): IntakeResponse? = intakePort.findByCaseIdAndPhase(caseId, phase)

	@Transactional
	override fun resubmit(
		caseId: UUID,
		customerId: UUID,
	): OnboardingCase {
		val case = requireCase(caseId)
		requireCustomerOwns(case, customerId)
		if (case.status != CaseStatus.REVISION_REQUESTED) {
			throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)
		}
		if (adapter.countOpenRevisionsByCaseId(caseId) > 0) {
			throw ArkException(ArkGlobalErrorCode.CONFLICT)
		}
		val returnTo =
			case.revisionRequestedFrom
				?: throw ArkException(ArkGlobalErrorCode.INVALID_INPUT)

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
		adapter.findById(caseId) ?: throw ArkException(ArkGlobalErrorCode.RESOURCE_NOT_FOUND)

	private fun requireCustomerOwns(
		case: OnboardingCase,
		customerId: UUID,
	) {
		if (case.customerId != customerId) {
			throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
		}
	}

	private fun requireRole(
		staff: AuthenticatedStaff,
		vararg roles: String,
	) {
		if (staff.role !in roles) {
			throw ArkException(ArkGlobalErrorCode.FORBIDDEN)
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
