package com.sentbe.bizplatform.arc.global.response

import com.sentbe.bizplatform.arc.global.trace.TraceContext
import java.time.OffsetDateTime

data class ApiResponse<T>(
	val traceNo: String?,
	val statusCode: Int,
	val resultCode: String,
	val resultMessage: String,
	val requestDateTime: OffsetDateTime?,
	val responseDateTime: OffsetDateTime,
	val data: T?,
	val path: String?,
	val exception: String?,
) {
	companion object {
		fun <T> okData(data: T): ApiResponse<T> =
			ApiResponse(
				traceNo = TraceContext.traceNo,
				statusCode = 200,
				resultCode = "SUCCESS",
				resultMessage = "success",
				requestDateTime = TraceContext.requestDateTime,
				responseDateTime = OffsetDateTime.now(),
				data = data,
				path = TraceContext.path,
				exception = null,
			)

		fun ok(): ApiResponse<Unit> =
			ApiResponse(
				traceNo = TraceContext.traceNo,
				statusCode = 200,
				resultCode = "SUCCESS",
				resultMessage = "success",
				requestDateTime = TraceContext.requestDateTime,
				responseDateTime = OffsetDateTime.now(),
				data = null,
				path = TraceContext.path,
				exception = null,
			)

		fun <T> exception(
			statusCode: Int,
			resultCode: String,
			message: String,
			exceptionDetail: String? = null,
		): ApiResponse<T> =
			ApiResponse(
				traceNo = TraceContext.traceNo,
				statusCode = statusCode,
				resultCode = resultCode,
				resultMessage = message,
				requestDateTime = TraceContext.requestDateTime,
				responseDateTime = OffsetDateTime.now(),
				data = null,
				path = TraceContext.path,
				exception = exceptionDetail,
			)
	}
}
