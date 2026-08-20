package com.sentbe.bizplatform.ark.case.exception

import com.sentbe.bizplatform.ark.global.exception.ArkErrorCode
import org.springframework.http.HttpStatus

enum class ArkCaseErrorCode(
	override val httpStatus: HttpStatus,
	override val code: String,
	override val message: String,
) : ArkErrorCode {
	CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "케이스를 찾을 수 없습니다"),
	CASE_FORBIDDEN(HttpStatus.FORBIDDEN, "C002", "케이스에 대한 접근 권한이 없습니다"),
	INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "C003", "유효하지 않은 상태 전이입니다"),
	REVISION_PENDING(HttpStatus.CONFLICT, "C004", "미해결 보완 요청이 남아 있습니다"),
	RESUBMIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "C005", "보완 재제출이 불가능한 상태입니다"),
	CLOSE_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "C006", "종료 사유는 필수입니다"),
	INVALID_CLOSE_REASON(HttpStatus.BAD_REQUEST, "C007", "유효하지 않은 종료 사유입니다"),
}
