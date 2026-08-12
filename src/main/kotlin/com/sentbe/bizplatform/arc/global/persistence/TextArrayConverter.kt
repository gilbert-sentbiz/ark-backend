package com.sentbe.bizplatform.arc.global.persistence

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component
import java.sql.Array

@WritingConverter
@Component
class StringListToArrayConverter : Converter<List<String>, Array> {
    override fun convert(source: List<String>): Array =
        throw UnsupportedOperationException(
            "text[] write requires JdbcOperations.createArrayOf — use TextArrayWriteSupport",
        )
}

@ReadingConverter
@Component
class ArrayToStringListConverter : Converter<Array, List<String>> {
    override fun convert(source: Array): List<String> =
        @Suppress("UNCHECKED_CAST")
        (source.array as kotlin.Array<Any?>).filterNotNull().map { it.toString() }
}
