package com.sentbe.bizplatform.arc.global.exception

import org.springframework.http.HttpStatus

interface ArcErrorCode {
	val httpStatus: HttpStatus
	val code: String
	val message: String
	val statusCode: Int get() = httpStatus.value()
}
