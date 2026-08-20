package com.sentbe.bizplatform.ark.fixtures

import com.sentbe.bizplatform.ark.case.application.domain.CaseStatus
import com.sentbe.bizplatform.ark.case.application.domain.OnboardingCase
import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.staff.application.domain.Staff
import java.time.OffsetDateTime
import java.util.UUID

object TestModels {
	val CUSTOMER_ID: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
	val STAFF_ID: UUID = UUID.fromString("00000001-0001-0000-0000-000000000000")
	val CASE_ID: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")

	fun aCustomer(
		id: UUID = CUSTOMER_ID,
		email: String = "test@example.com",
	) = Customer(id = id, email = email, createdAt = OffsetDateTime.now())

	fun aStaff(
		id: UUID = STAFF_ID,
		email: String = "sales@sentbe.com",
		role: String = "SALES",
	) = Staff(id = id, email = email, name = "테스트 직원", role = role, isActive = true, createdAt = OffsetDateTime.now())

	fun aCase(
		id: UUID = CASE_ID,
		customerId: UUID = CUSTOMER_ID,
		status: String = CaseStatus.INQUIRY_RECEIVED,
	) = OnboardingCase(
		id = id,
		customerId = customerId,
		status = status,
		services = emptyList(),
		sectors = emptyList(),
		segmentMeta = emptyMap(),
		pinnedQuestionIds = emptyMap(),
	)
}
