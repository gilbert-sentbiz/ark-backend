package com.sentbe.bizplatform.ark.global.auth

import com.sentbe.bizplatform.ark.customer.adapter.out.CustomerRepository
import com.sentbe.bizplatform.ark.customer.adapter.out.CustomerSessionRepository
import com.sentbe.bizplatform.ark.staff.adapter.out.StaffRepository
import com.sentbe.bizplatform.ark.staff.adapter.out.StaffSessionRepository
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class FilterConfig(
	private val customerSessionRepo: CustomerSessionRepository,
	private val customerRepo: CustomerRepository,
	private val staffSessionRepo: StaffSessionRepository,
	private val staffRepo: StaffRepository,
) {
	@Bean
	fun customerAuthFilter(): FilterRegistrationBean<CustomerAuthFilter> =
		FilterRegistrationBean(CustomerAuthFilter(customerSessionRepo, customerRepo)).apply {
			order = Ordered.HIGHEST_PRECEDENCE + 10
			addUrlPatterns("/*")
		}

	@Bean
	fun staffAuthFilter(): FilterRegistrationBean<StaffAuthFilter> =
		FilterRegistrationBean(StaffAuthFilter(staffSessionRepo, staffRepo)).apply {
			order = Ordered.HIGHEST_PRECEDENCE + 20
			addUrlPatterns("/internal/*", "/rules/*")
		}
}
