package com.sentbe.bizplatform.ark.customer.application.service

import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.ark.customer.application.port.`in`.CustomerAuthPort
import com.sentbe.bizplatform.ark.customer.application.port.out.CustomerOutPort
import com.sentbe.bizplatform.ark.global.auth.OtpSender
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CustomerAuthService(
	private val outPort: CustomerOutPort,
	private val redis: StringRedisTemplate,
	private val otpSender: OtpSender,
	@Value("\${ark.auth.otp-ttl-seconds:300}") private val otpTtlSeconds: Long,
	@Value("\${ark.auth.session-hours:72}") private val sessionHours: Long,
) : CustomerAuthPort {
	private val random = SecureRandom()

	override fun requestOtp(email: String) {
		val code = String.format("%06d", random.nextInt(1_000_000))
		redis.opsForValue().set(otpKey(email), code, Duration.ofSeconds(otpTtlSeconds))
		otpSender.send(email, code)
	}

	@Transactional
	override fun verifyOtp(
		email: String,
		code: String,
	): String {
		val stored =
			redis.opsForValue().get(otpKey(email))
				?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
		if (stored != code) throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)

		redis.delete(otpKey(email))

		val customer =
			outPort.findByEmail(email)
				?: outPort.saveCustomer(Customer(email = email))

		val token = UUID.randomUUID().toString()
		outPort.saveSession(
			CustomerSession(
				customerId = customer.id!!,
				token = token,
				expiresAt = OffsetDateTime.now().plusHours(sessionHours),
			),
		)
		return token
	}

	private fun otpKey(email: String) = "otp:$email"
}
