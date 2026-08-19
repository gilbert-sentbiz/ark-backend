package com.sentbe.bizplatform.arc.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
	@ExceptionHandler(IllegalArgumentException::class)
	fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "잘못된 요청입니다")

	@ExceptionHandler(NoSuchElementException::class)
	fun handleNotFound(ex: NoSuchElementException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "리소스를 찾을 수 없습니다")

	@ExceptionHandler(IllegalStateException::class)
	fun handleConflict(ex: IllegalStateException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "상태 전이 오류입니다")
}
