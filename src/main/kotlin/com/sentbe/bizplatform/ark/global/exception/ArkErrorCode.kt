package com.sentbe.bizplatform.ark.global.exception

import org.springframework.http.HttpStatus

interface ArkErrorCode {
	val httpStatus: HttpStatus
	val code: String
	val message: String
	val statusCode: Int get() = httpStatus.value()
}
