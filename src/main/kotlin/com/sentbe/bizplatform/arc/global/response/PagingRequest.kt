package com.sentbe.bizplatform.arc.global.response

import jakarta.validation.constraints.Min

data class PagingRequest(
	@field:Min(1) val unit: Int = 20,
	val isPageable: Boolean = true,
	@field:Min(0) val offset: Long = 0,
) {
	init {
		require(unit > 0) { "unit must be greater than 0" }
		require(offset >= 0) { "offset must be non-negative" }
	}
}
