package com.sentbe.bizplatform.ark.global.persistence

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import java.sql.Array

@ReadingConverter
class ArrayToStringListConverter : Converter<Array, List<String>> {
	override fun convert(source: Array): List<String> =
		@Suppress("UNCHECKED_CAST")
		(source.array as kotlin.Array<Any?>).filterNotNull().map { it.toString() }
}
