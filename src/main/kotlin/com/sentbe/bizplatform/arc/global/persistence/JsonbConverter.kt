package com.sentbe.bizplatform.arc.global.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

private val MAPPER = ObjectMapper().findAndRegisterModules()
private val MAP_TYPE = object : TypeReference<Map<String, Any>>() {}
private val LIST_TYPE = object : TypeReference<List<Any>>() {}

@WritingConverter
class MapToJsonbConverter : Converter<Map<*, *>, PGobject> {
    override fun convert(source: Map<*, *>): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = MAPPER.writeValueAsString(source)
        }
}

@ReadingConverter
class JsonbToMapConverter : Converter<PGobject, Map<String, Any>> {
    override fun convert(source: PGobject): Map<String, Any> = MAPPER.readValue(source.value ?: "{}", MAP_TYPE)
}

@WritingConverter
class ListToJsonbConverter : Converter<List<*>, PGobject> {
    override fun convert(source: List<*>): PGobject =
        PGobject().apply {
            type = "jsonb"
            value = MAPPER.writeValueAsString(source)
        }
}

@ReadingConverter
class JsonbToListConverter : Converter<PGobject, List<Any>> {
    override fun convert(source: PGobject): List<Any> = MAPPER.readValue(source.value ?: "[]", LIST_TYPE)
}
