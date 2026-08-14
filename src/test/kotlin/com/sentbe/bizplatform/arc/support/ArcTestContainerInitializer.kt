package com.sentbe.bizplatform.arc.support

import liquibase.integration.spring.SpringLiquibase
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

object ArcTestContainers {
    init {
        // Docker-in-Docker 환경(QA 등)에서 Ryuk 컨테이너 기동 실패 방지
        System.setProperty("testcontainers.ryuk.disabled", "true")
    }

    val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .also { it.start() }

    val redis: GenericContainer<*> =
        GenericContainer<Nothing>("redis:7-alpine")
            .withExposedPorts(6379)
            .also { it.start() }
}

// Liquibase migrations 최초 1회만 실행 (object 초기화 시점)
private object LiquibaseMigrations {
    init {
        val pg = ArcTestContainers.postgres
        val ds =
            SimpleDriverDataSource(
                Class.forName("org.postgresql.Driver").getDeclaredConstructor().newInstance() as java.sql.Driver,
                pg.jdbcUrl,
                pg.username,
                pg.password,
            )
        val liq = SpringLiquibase()
        liq.dataSource = ds
        liq.changeLog = "classpath:db/changelog/db.changelog-master.xml"
        liq.afterPropertiesSet()
    }
}

class ArcTestContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(ctx: ConfigurableApplicationContext) {
        @Suppress("UNUSED_EXPRESSION")
        LiquibaseMigrations // 최초 접근 시 마이그레이션 실행
        TestPropertyValues
            .of(
                "spring.datasource.url=${ArcTestContainers.postgres.jdbcUrl}",
                "spring.datasource.username=${ArcTestContainers.postgres.username}",
                "spring.datasource.password=${ArcTestContainers.postgres.password}",
                "spring.data.redis.host=${ArcTestContainers.redis.host}",
                "spring.data.redis.port=${ArcTestContainers.redis.getMappedPort(6379)}",
                "spring.liquibase.enabled=false",
            ).applyTo(ctx)
    }
}
