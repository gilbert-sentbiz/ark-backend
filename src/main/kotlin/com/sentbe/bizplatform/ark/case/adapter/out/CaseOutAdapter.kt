package com.sentbe.bizplatform.ark.case.adapter.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.ark.case.application.port.out.CaseOutPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

private val MAPPER = ObjectMapper().findAndRegisterModules()

@Component
class CaseOutAdapter(
	private val jdbc: JdbcClient,
	private val caseRepository: OnboardingCaseRepository,
	private val caseEventRepository: CaseEventRepository,
) : CaseOutPort {
	override fun save(case: OnboardingCase): OnboardingCase {
		val exists =
			jdbc
				.sql("SELECT 1 FROM onboarding_case WHERE id = :id")
				.param("id", case.id)
				.query(Int::class.java)
				.optional()
				.isPresent

		if (exists) {
			jdbc
				.sql(
					"""UPDATE onboarding_case
                   SET status = :status, close_reason = :closeReason,
                       revision_requested_from = :revisionRequestedFrom,
                       entity_code = :entityCode,
                       services = :services::text[], sectors = :sectors::text[],
                       segment_meta = :segmentMeta::jsonb,
                       pinned_question_ids = :pinnedQuestionIds::jsonb,
                       assignee_staff_id = :assigneeStaffId,
                       last_customer_action_at = :lastCustomerActionAt,
                       updated_at = now()
                   WHERE id = :id""",
				).param("id", case.id)
				.param("status", case.status)
				.param("closeReason", case.closeReason)
				.param("revisionRequestedFrom", case.revisionRequestedFrom)
				.param("entityCode", case.entityCode)
				.param("services", case.services.toPgArray())
				.param("sectors", case.sectors.toPgArray())
				.param("segmentMeta", MAPPER.writeValueAsString(case.segmentMeta))
				.param("pinnedQuestionIds", MAPPER.writeValueAsString(case.pinnedQuestionIds))
				.param("assigneeStaffId", case.assigneeStaffId)
				.param("lastCustomerActionAt", case.lastCustomerActionAt)
				.update()
		} else {
			jdbc
				.sql(
					"""INSERT INTO onboarding_case
                   (id, customer_id, status, services, sectors, segment_meta, pinned_question_ids)
                   VALUES (:id, :customerId, :status, :services::text[], :sectors::text[],
                           :segmentMeta::jsonb, :pinnedQuestionIds::jsonb)""",
				).param("id", case.id)
				.param("customerId", case.customerId)
				.param("status", case.status)
				.param("services", case.services.toPgArray())
				.param("sectors", case.sectors.toPgArray())
				.param("segmentMeta", MAPPER.writeValueAsString(case.segmentMeta))
				.param("pinnedQuestionIds", MAPPER.writeValueAsString(case.pinnedQuestionIds))
				.update()
		}
		return findById(case.id)!!
	}

	override fun findById(id: UUID): OnboardingCase? = caseRepository.findById(id).orElse(null)?.toDomain()

	override fun findByCustomerId(customerId: UUID): OnboardingCase? =
		caseRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId)?.toDomain()

	override fun findAllForDashboard(): List<OnboardingCase> = caseRepository.findAllForDashboard().map { it.toDomain() }

	override fun findCaseEvents(caseId: UUID): List<Map<String, Any>> =
		caseEventRepository.findByCaseIdOrderByCreatedAt(caseId).map { entity ->
			mapOf<String, Any>(
				"id" to entity.id.toString(),
				"eventType" to entity.eventType,
				"actorType" to entity.actorType,
				"actorId" to (entity.actorId?.toString() ?: ""),
				"payload" to MAPPER.writeValueAsString(entity.payload),
				"createdAt" to (entity.createdAt?.toString() ?: ""),
			)
		}

	override fun createDocumentsForCase(
		caseId: UUID,
		docTemplates: List<Pair<UUID, Map<String, Any>>>,
	) {
		docTemplates.forEach { (templateId, info) ->
			jdbc
				.sql(
					"""INSERT INTO document (case_id, doc_template_id, type, display_name, is_required, is_conditional)
                   VALUES (:caseId, :templateId, :type, :displayName, :isRequired, :isConditional)
                   ON CONFLICT (case_id, type) DO NOTHING""",
				).param("caseId", caseId)
				.param("templateId", templateId)
				.param("type", info["type"] as String)
				.param("displayName", info["displayName"] as String)
				.param("isRequired", info["isRequired"] as Boolean)
				.param("isConditional", info["isConditional"] as Boolean)
				.update()
		}
	}

	override fun countOpenRevisionsByCaseId(caseId: UUID): Int =
		jdbc
			.sql(
				"""SELECT COUNT(*) FROM revision_request rr
               JOIN document d ON rr.document_id = d.id
               WHERE d.case_id = :caseId AND rr.resolved_at IS NULL""",
			).param("caseId", caseId)
			.query(Int::class.java)
			.single()

	private fun OnboardingCaseJdbcEntity.toDomain() =
		OnboardingCase(
			id = id,
			customerId = customerId,
			status = status,
			closeReason = closeReason,
			revisionRequestedFrom = revisionRequestedFrom,
			entityCode = entityCode,
			services = services,
			sectors = sectors,
			segmentMeta = segmentMeta,
			pinnedQuestionIds = pinnedQuestionIds,
			assigneeStaffId = assigneeStaffId,
			lastCustomerActionAt = lastCustomerActionAt,
			createdAt = createdAt ?: OffsetDateTime.now(),
			updatedAt = updatedAt ?: OffsetDateTime.now(),
		)

	private fun List<String>.toPgArray(): String = "{${joinToString(",") { it.replace(",", "\\,") }}}"
}
