package com.sentbe.bizplatform.arc.global.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@WritingConverter
@Component
class MapToJsonbConverter(private val mapper: ObjectMapper) : Converter<Map<*, *>, PGobject> {
    override fun convert(source: Map<*, *>): PGobject =
        PGobject().apply { type = "jsonb"; value = mapper.writeValueAsString(source) }
}

@ReadingConverter
@Component
class JsonbToMapConverter(private val mapper: ObjectMapper) : Converter<PGobject, Map<String, Any>> {
    override fun convert(source: PGobject): Map<String, Any> =
        mapper.readValue(source.value ?: "{}")
}

@WritingConverter
@Component
class ListToJsonbConverter(private val mapper: ObjectMapper) : Converter<List<*>, PGobject> {
    override fun convert(source: List<*>): PGobject =
        PGobject().apply { type = "jsonb"; value = mapper.writeValueAsString(source) }
}

@ReadingConverter
@Component
class JsonbToListConverter(private val mapper: ObjectMapper) : Converter<PGobject, List<Any>> {
    override fun convert(source: PGobject): List<Any> =
        mapper.readValue(source.value ?: "[]")
}
