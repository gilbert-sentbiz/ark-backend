package com.sentbe.bizplatform.ark.global.persistence

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.Connection

/**
 * Spring Data JDBC does not natively write List<String> as PostgreSQL text[].
 * Use this helper when an aggregate has a text[] column (services, sectors).
 */
@Component
class TextArrayWriteSupport(
	private val jdbc: JdbcClient,
) {
	fun toSqlArray(
		conn: Connection,
		values: List<String>,
	): java.sql.Array = conn.createArrayOf("text", values.toTypedArray())
}
