package com.sentbe.bizplatform.arc.global.response

data class PagingResponse(
	val total: Long,
	val unit: Int,
	val offset: Long,
	val hasNext: Boolean,
) {
	companion object {
		fun of(
			total: Long,
			unit: Int,
			offset: Long,
		): PagingResponse =
			PagingResponse(
				total = total,
				unit = unit,
				offset = offset,
				hasNext = offset + unit < total,
			)
	}
}
