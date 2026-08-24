package com.sentbe.bizplatform.ark.customer.application.service

import com.sentbe.bizplatform.ark.customer.application.domain.Customer
import com.sentbe.bizplatform.ark.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.ark.customer.application.port.`in`.CustomerAuthPort
import com.sentbe.bizplatform.ark.customer.application.port.out.CustomerOutPort
import com.sentbe.bizplatform.ark.global.auth.OtpSender
import com.sentbe.bizplatform.ark.global.exception.ArkException
import com.sentbe.bizplatform.ark.global.exception.ArkGlobalErrorCode
import org.apache.logging.log4j.LogManager
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
	// 로컬 데모 만능 인증코드. 기본 빈 문자열 = 비활성. application-local.yaml에서만 설정하며
	// dev/stg/prd 프로파일에는 없으므로 운영에서는 절대 우회되지 않는다.
	@Value("\${ark.auth.otp-master-code:}") private val otpMasterCode: String,
) : CustomerAuthPort {
	private val random = SecureRandom()
	private val log = LogManager.getLogger(CustomerAuthService::class.java)

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
		// 로컬 데모 만능키: 설정된 경우에만(=local 프로파일), OTP 요청 없이도 이 코드로 통과.
		val masterEnabled = otpMasterCode.isNotBlank() && code == otpMasterCode
		if (masterEnabled) {
			log.warn("[OTP-MASTER] 로컬 데모 만능키로 인증됨 — email={} (운영 프로파일에서는 비활성)", email)
		} else {
			val stored =
				redis.opsForValue().get(otpKey(email))
					?: throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
			if (stored != code) throw ArkException(ArkGlobalErrorCode.UNAUTHORIZED)
			redis.delete(otpKey(email))
		}

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
