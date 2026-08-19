package com.sentbe.bizplatform.arc.global.exception

import org.springframework.http.HttpStatus

// 도메인 코드 대역 규칙
// G: Global   G001~G999
// C: Case     C001~C999
// D: Document D001~D999
// U: Customer U001~U999
// S: Staff    S001~S999
// R: Rule     R001~R999
enum class ArcGlobalErrorCode(
	override val httpStatus: HttpStatus,
	override val code: String,
	override val message: String,
) : ArcErrorCode {
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "G001", "입력값이 올바르지 않습니다"),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "G002", "리소스를 찾을 수 없습니다"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "G003", "인증이 필요합니다"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "G004", "접근 권한이 없습니다"),
	METHOD_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, "G005", "지원하지 않는 HTTP 메서드입니다"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "G006", "지원하지 않는 미디어 타입입니다"),
	CONFLICT(HttpStatus.CONFLICT, "G007", "요청을 처리할 수 없는 상태입니다"),
	MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "G008", "요청 본문을 읽을 수 없습니다"),
	DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G009", "데이터 접근 중 오류가 발생했습니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G999", "서버 내부 오류가 발생했습니다"),
}
