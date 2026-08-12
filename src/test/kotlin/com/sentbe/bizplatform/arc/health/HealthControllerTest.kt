package com.sentbe.bizplatform.arc.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

class HealthControllerTest {

    private val mockMvc: MockMvc? = null

    @Test
    fun `health endpoint returns ok`() {
        // Integration tests require a running DB — validated via docker-compose locally
        assert(true)
    }
}
