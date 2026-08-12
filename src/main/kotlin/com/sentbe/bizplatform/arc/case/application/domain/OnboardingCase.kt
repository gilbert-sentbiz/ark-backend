package com.sentbe.bizplatform.arc.case.application.domain

import java.time.OffsetDateTime
import java.util.UUID

data class OnboardingCase(
    val id: UUID,
    val customerId: UUID,
    val status: String,
    val closeReason: String? = null,
    val revisionRequestedFrom: String? = null,
    val entityCode: String? = null,
    val services: List<String> = emptyList(),
    val sectors: List<String> = emptyList(),
    val segmentMeta: Map<String, Any> = emptyMap(),
    val pinnedQuestionIds: Map<String, Any> = emptyMap(),
    val assigneeStaffId: UUID? = null,
    val lastCustomerActionAt: OffsetDateTime? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

object CaseStatus {
    const val INQUIRY_RECEIVED = "INQUIRY_RECEIVED"
    const val DOCUMENT_SUBMISSION_REQUIRED = "DOCUMENT_SUBMISSION_REQUIRED"
    const val INITIAL_SCREENING = "INITIAL_SCREENING"
    const val DOCUMENT_SCREENING_REQUIRED = "DOCUMENT_SCREENING_REQUIRED"
    const val APPROVAL_REVIEW_REQUIRED = "APPROVAL_REVIEW_REQUIRED"
    const val ACCOUNT_SETUP_REQUIRED = "ACCOUNT_SETUP_REQUIRED"
    const val REVISION_REQUESTED = "REVISION_REQUESTED"
    const val COMPLETED = "COMPLETED"
    const val CLOSED = "CLOSED"
}
