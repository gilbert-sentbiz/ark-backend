package com.sentbe.bizplatform.ark.document.application.service

import com.sentbe.bizplatform.ark.global.config.CredentialSource
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class S3StorageService(
	private val s3: S3Client,
	private val credentials: CredentialSource,
) {
	fun upload(
		key: String,
		bytes: ByteArray,
		contentType: String,
	) {
		val request =
			PutObjectRequest
				.builder()
				.bucket(credentials.s3Bucket())
				.key(key)
				.contentType(contentType)
				.contentLength(bytes.size.toLong())
				.build()
		s3.putObject(request, RequestBody.fromBytes(bytes))
	}
}
