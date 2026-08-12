package com.sentbe.bizplatform.arc.global.persistence

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration

@Configuration
class JdbcConfig(
    private val mapToJsonb: MapToJsonbConverter,
    private val jsonbToMap: JsonbToMapConverter,
    private val listToJsonb: ListToJsonbConverter,
    private val jsonbToList: JsonbToListConverter,
    private val arrayToList: ArrayToStringListConverter,
) : AbstractJdbcConfiguration() {
    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions =
        JdbcCustomConversions(
            listOf(mapToJsonb, jsonbToMap, listToJsonb, jsonbToList, arrayToList),
        )
}
