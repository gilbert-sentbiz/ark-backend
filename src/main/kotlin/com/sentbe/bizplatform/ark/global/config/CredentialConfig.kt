package com.sentbe.bizplatform.ark.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

/**
 * 크리덴셜 추상화.
 *
 * - local 프로필: 환경변수(.env)에서 직접 읽음
 * - dev/stg/prd 프로필: AWS Secrets Manager에서 읽음
 *
 * 코드 변경 없이 Spring 프로필로만 소스 전환.
 */
interface CredentialSource {
	fun dbUrl(): String

	fun dbUsername(): String

	fun dbPassword(): String

	fun s3Endpoint(): String?

	fun s3Bucket(): String

	fun s3AccessKey(): String

	fun s3SecretKey(): String

	fun redisHost(): String

	fun redisPort(): Int
}

@Configuration
@Profile("local")
class EnvCredentialConfig {
	@Bean
	fun credentialSource(
		@Value("\${spring.datasource.url}") dbUrl: String,
		@Value("\${spring.datasource.username}") dbUser: String,
		@Value("\${spring.datasource.password}") dbPass: String,
		@Value("\${ark.s3.endpoint:}") s3Endpoint: String,
		@Value("\${ark.s3.bucket}") s3Bucket: String,
		@Value("\${ark.s3.access-key}") s3AccessKey: String,
		@Value("\${ark.s3.secret-key}") s3SecretKey: String,
		@Value("\${spring.data.redis.host}") redisHost: String,
		@Value("\${spring.data.redis.port}") redisPort: Int,
	): CredentialSource =
		object : CredentialSource {
			override fun dbUrl() = dbUrl

			override fun dbUsername() = dbUser

			override fun dbPassword() = dbPass

			override fun s3Endpoint() = s3Endpoint.ifBlank { null }

			override fun s3Bucket() = s3Bucket

			override fun s3AccessKey() = s3AccessKey

			override fun s3SecretKey() = s3SecretKey

			override fun redisHost() = redisHost

			override fun redisPort() = redisPort
		}
}

@Configuration
@Profile("dev", "stg", "prd")
class AwsCredentialConfig(
	@Value("\${ark.secrets.manager.secret-id}") private val secretId: String,
	@Value("\${ark.secrets.manager.region}") private val awsRegion: String,
) {
	@Bean
	fun credentialSource(mapper: ObjectMapper): CredentialSource {
		val client = SecretsManagerClient.builder().region(Region.of(awsRegion)).build()
		val json = client.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString()
		val map = mapper.readValue(json, Map::class.java) as Map<*, *>

		fun str(key: String) = map[key]?.toString() ?: error("Missing secret key: $key")

		fun strOrNull(key: String) = map[key]?.toString()
		return object : CredentialSource {
			override fun dbUrl() = str("DB_URL")

			override fun dbUsername() = str("DB_USERNAME")

			override fun dbPassword() = str("DB_PASSWORD")

			override fun s3Endpoint() = strOrNull("S3_ENDPOINT")

			override fun s3Bucket() = str("S3_BUCKET")

			override fun s3AccessKey() = str("S3_ACCESS_KEY")

			override fun s3SecretKey() = str("S3_SECRET_KEY")

			override fun redisHost() = str("REDIS_HOST")

			override fun redisPort() = str("REDIS_PORT").toInt()
		}
	}
}
