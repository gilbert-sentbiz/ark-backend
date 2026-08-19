package com.sentbe.bizplatform.arc.global.exception

class ArcException(
	val errorCode: ArcErrorCode,
	val customMessage: String? = null,
	val data: Any? = null,
	cause: Throwable? = null,
) : RuntimeException(customMessage ?: errorCode.message, cause)
