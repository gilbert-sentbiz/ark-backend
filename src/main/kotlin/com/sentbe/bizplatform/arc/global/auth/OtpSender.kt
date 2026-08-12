package com.sentbe.bizplatform.arc.global.auth

import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

interface OtpSender {
    fun send(
        email: String,
        code: String,
    )
}

@Component
@Profile("local")
class ConsoleOtpSender : OtpSender {
    private val log = LogManager.getLogger(ConsoleOtpSender::class.java)

    override fun send(
        email: String,
        code: String,
    ) {
        log.info("[OTP-MOCK] email={} code={}", email, code)
    }
}

@Component
@Profile("dev", "stg", "prd")
class MailOtpSender : OtpSender {
    private val log = LogManager.getLogger(MailOtpSender::class.java)

    override fun send(
        email: String,
        code: String,
    ) {
        // TODO: wire JavaMailSender from CredentialSource once SMTP config is provisioned
        log.warn("[OTP-MAIL] stub — email={}", email)
        throw UnsupportedOperationException("Mail OTP sender not configured")
    }
}
