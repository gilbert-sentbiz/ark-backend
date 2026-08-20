package com.sentbe.bizplatform.ark.intake.adapter.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.sentbe.bizplatform.ark.case.application.domain.IntakeResponse
import com.sentbe.bizplatform.ark.intake.application.port.out.IntakeOutPort
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

private val MAPPER = ObjectMapper().findAndRegisterModules()

@Component
class IntakeOutAdapter(
	private val jdbc: JdbcClient,
	private val intakeRepository: IntakeResponseRepository,
) : IntakeOutPort {
	override fun save(intake: IntakeResponse): IntakeResponse {
		jdbc
			.sql(
				"""INSERT INTO intake_response (case_id, phase, status, answers, submitted_at)
               VALUES (:caseId, :phase, :status, :answers::jsonb, :submittedAt)
               ON CONFLICT (case_id, phase) DO UPDATE
               SET status = EXCLUDED.status, answers = EXCLUDED.answers,
                   saved_at = now(), submitted_at = EXCLUDED.submitted_at""",
			).param("caseId", intake.caseId)
			.param("phase", intake.phase)
			.param("status", intake.status)
			.param("answers", MAPPER.writeValueAsString(intake.answers))
			.param("submittedAt", intake.submittedAt)
			.update()
		return findByCaseIdAndPhase(intake.caseId, intake.phase)!!
	}

	override fun findByCaseIdAndPhase(
		caseId: UUID,
		phase: String,
	): IntakeResponse? = intakeRepository.findByCaseIdAndPhase(caseId, phase)?.toDomain()

	private fun IntakeResponseJdbcEntity.toDomain() =
		IntakeResponse(
			id = id ?: UUID.randomUUID(),
			caseId = caseId,
			phase = phase,
			status = status,
			answers = answers,
			savedAt = savedAt ?: OffsetDateTime.now(),
			submittedAt = submittedAt,
		)
}
