package com.sentbe.bizplatform.arc.customer.application.service

import com.sentbe.bizplatform.arc.customer.application.domain.Customer
import com.sentbe.bizplatform.arc.customer.application.domain.CustomerSession
import com.sentbe.bizplatform.arc.customer.application.port.`in`.CustomerAuthUseCase
import com.sentbe.bizplatform.arc.customer.application.port.out.CustomerOutPort
import com.sentbe.bizplatform.arc.global.auth.OtpSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CustomerAuthService(
    private val outPort: CustomerOutPort,
    private val redis: StringRedisTemplate,
    private val otpSender: OtpSender,
    @Value("\${arc.auth.otp-ttl-seconds:300}") private val otpTtlSeconds: Long,
    @Value("\${arc.auth.session-hours:72}") private val sessionHours: Long,
) : CustomerAuthUseCase {
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
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP 만료 또는 미발급")
        if (stored != code) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP 불일치")

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
