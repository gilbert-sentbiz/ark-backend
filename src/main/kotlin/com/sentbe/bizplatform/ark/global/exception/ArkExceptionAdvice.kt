package com.sentbe.bizplatform.ark.global.exception

import com.sentbe.bizplatform.ark.global.response.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.sql.SQLException

@RestControllerAdvice
class ArkExceptionAdvice {
	@ExceptionHandler(ArkException::class)
	fun handleArkException(ex: ArkException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ex.errorCode
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, ex.message ?: code.message, ex.data?.toString()))
	}

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
		val detail = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
		val code = ArkGlobalErrorCode.INVALID_INPUT
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message, detail))
	}

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
		val detail = ex.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
		val code = ArkGlobalErrorCode.INVALID_INPUT
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message, detail))
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.INVALID_INPUT
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message, "파라미터 타입 불일치: ${ex.name}"))
	}

	// PI-240: 업로드 파일이 multipart 한도(10MB) 초과 시 net::ERR_FAILED 대신
	// 명확한 413 JSON 응답. @RestControllerAdvice라 CORS 헤더도 정상 포함됨.
	@ExceptionHandler(MaxUploadSizeExceededException::class)
	fun handleMaxUploadSize(ex: MaxUploadSizeExceededException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.INVALID_INPUT
		return ResponseEntity
			.status(HttpStatus.PAYLOAD_TOO_LARGE)
			.body(ApiResponse.exception(code.statusCode, code.code, "파일 크기는 10MB를 초과할 수 없습니다."))
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.MESSAGE_NOT_READABLE
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException::class)
	fun handleMediaType(ex: HttpMediaTypeNotSupportedException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.UNSUPPORTED_MEDIA_TYPE
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException::class)
	fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.METHOD_NOT_SUPPORTED
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNoHandler(ex: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.RESOURCE_NOT_FOUND
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(DataAccessException::class)
	fun handleDataAccess(ex: DataAccessException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.DATA_ACCESS_ERROR
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(SQLException::class)
	fun handleSql(ex: SQLException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.DATA_ACCESS_ERROR
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(RuntimeException::class)
	fun handleRuntime(ex: RuntimeException): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.INTERNAL_SERVER_ERROR
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}

	@ExceptionHandler(Exception::class)
	fun handleAll(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
		val code = ArkGlobalErrorCode.INTERNAL_SERVER_ERROR
		return ResponseEntity
			.status(code.httpStatus)
			.body(ApiResponse.exception(code.statusCode, code.code, code.message))
	}
}
