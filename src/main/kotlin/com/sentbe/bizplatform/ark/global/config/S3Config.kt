package com.sentbe.bizplatform.ark.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config(
	private val credentials: CredentialSource,
) {
	@Bean
	fun s3Client(): S3Client {
		val builder =
			S3Client
				.builder()
				.credentialsProvider(
					StaticCredentialsProvider.create(
						AwsBasicCredentials.create(credentials.s3AccessKey(), credentials.s3SecretKey()),
					),
				).region(Region.AP_NORTHEAST_2)

		val endpoint = credentials.s3Endpoint()
		if (endpoint != null) {
			builder.endpointOverride(URI.create(endpoint))
			builder.forcePathStyle(true)
		}

		return builder.build()
	}
}
