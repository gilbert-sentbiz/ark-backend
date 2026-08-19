package com.sentbe.bizplatform.arc.global.persistence

import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ReadingConverter
class TimestampToOffsetDateTimeConverter : Converter<Timestamp, OffsetDateTime> {
	override fun convert(source: Timestamp): OffsetDateTime = source.toInstant().atOffset(ZoneOffset.UTC)
}

@ReadingConverter
class PGobjectToStringConverter : Converter<PGobject, String> {
	override fun convert(source: PGobject): String = source.value ?: ""
}

@Configuration
@EnableJdbcRepositories(basePackages = ["com.sentbe.bizplatform.arc"])
@EnableJdbcAuditing
class JdbcConfig : AbstractJdbcConfiguration() {
	@Bean
	override fun jdbcCustomConversions(): JdbcCustomConversions =
		JdbcCustomConversions(
			listOf(
				MapToJsonbConverter(),
				JsonbToMapConverter(),
				ListToJsonbConverter(),
				JsonbToListConverter(),
				ArrayToStringListConverter(),
				TimestampToOffsetDateTimeConverter(),
				PGobjectToStringConverter(),
			),
		)
}
