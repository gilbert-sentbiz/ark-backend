package com.sentbe.bizplatform.ark.global.response

data class ResultList<T>(
	val list: List<T>,
	val summary: Map<String, Any>? = null,
	val pagination: PagingResponse? = null,
)
