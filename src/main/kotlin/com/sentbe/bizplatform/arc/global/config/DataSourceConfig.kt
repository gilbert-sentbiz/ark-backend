package com.sentbe.bizplatform.arc.global.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import javax.sql.DataSource

@Configuration
@Profile("dev", "stg", "prd")
class DataSourceConfig {
	@Bean
	fun dataSource(credentials: CredentialSource): DataSource {
		val config = HikariConfig()
		config.jdbcUrl = credentials.dbUrl()
		config.username = credentials.dbUsername()
		config.password = credentials.dbPassword()
		config.driverClassName = "org.postgresql.Driver"
		return HikariDataSource(config)
	}

	@Bean
	fun redisConnectionFactory(credentials: CredentialSource): LettuceConnectionFactory =
		LettuceConnectionFactory(RedisStandaloneConfiguration(credentials.redisHost(), credentials.redisPort()))
}
