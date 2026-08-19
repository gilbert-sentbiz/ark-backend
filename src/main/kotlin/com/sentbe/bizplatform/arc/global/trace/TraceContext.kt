package com.sentbe.bizplatform.arc.global.trace

import java.time.OffsetDateTime

object TraceContext {
	private val TRACE_NO = ThreadLocal<String?>()
	private val PATH = ThreadLocal<String?>()
	private val REQUEST_DT = ThreadLocal<OffsetDateTime?>()

	val traceNo: String? get() = TRACE_NO.get()
	val path: String? get() = PATH.get()
	val requestDateTime: OffsetDateTime? get() = REQUEST_DT.get()

	fun set(
		traceNo: String,
		path: String,
		requestDateTime: OffsetDateTime,
	) {
		TRACE_NO.set(traceNo)
		PATH.set(path)
		REQUEST_DT.set(requestDateTime)
	}

	fun clear() {
		TRACE_NO.remove()
		PATH.remove()
		REQUEST_DT.remove()
	}
}
