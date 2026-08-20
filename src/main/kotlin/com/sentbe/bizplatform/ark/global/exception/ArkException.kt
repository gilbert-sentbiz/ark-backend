package com.sentbe.bizplatform.ark.global.exception

class ArkException(
	val errorCode: ArkErrorCode,
	val customMessage: String? = null,
	val data: Any? = null,
	cause: Throwable? = null,
) : RuntimeException(customMessage ?: errorCode.message, cause)
