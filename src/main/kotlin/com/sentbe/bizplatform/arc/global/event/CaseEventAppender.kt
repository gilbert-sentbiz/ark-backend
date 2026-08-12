package com.sentbe.bizplatform.arc.global.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

enum class EventType { CASE_CREATED, CASE_STATUS_CHANGED, DOC_STATUS_CHANGED, ASSIGNEE_CHANGED }

enum class ActorType { CUSTOMER, STAFF, SYSTEM }

data class Actor(
    val type: ActorType,
    val id: UUID? = null,
)

@Component
class CaseEventAppender(
    private val jdbc: JdbcClient,
    private val mapper: ObjectMapper,
) {
    fun append(
        caseId: UUID,
        eventType: EventType,
        actor: Actor,
        payload: Map<String, Any> = emptyMap(),
    ) {
        jdbc
            .sql(
                """INSERT INTO case_event (case_id, event_type, actor_type, actor_id, payload)
               VALUES (:caseId, :eventType, :actorType, :actorId, :payload::jsonb)""",
            ).param("caseId", caseId)
            .param("eventType", eventType.name)
            .param("actorType", actor.type.name)
            .param("actorId", actor.id)
            .param("payload", mapper.writeValueAsString(payload))
            .update()
    }
}
