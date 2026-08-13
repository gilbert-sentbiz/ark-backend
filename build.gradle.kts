import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.asciidoctor)
}

group = "com.sentbe.bizplatform"
version = "0.0.1-SNAPSHOT"

java {
    // Company standard is JDK 25; local machine has 26 — CI/prod uses JDK 25 toolchain
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    }
}

val snippetsDir = file("build/generated-snippets")

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    exclude(group = "ch.qos.logback", module = "logback-classic")
}

repositories {
    // 사내 Nexus — 크리덴셜 없으면 mavenCentral() 폴백
    val nexusUser = System.getenv("NEXUS_USERNAME")
    val nexusPass = System.getenv("NEXUS_PASSWORD")
    if (!nexusUser.isNullOrBlank()) {
        maven {
            url = uri("https://nexus.sentbe.com/repository/maven-public/")
            credentials {
                username = nexusUser
                password = nexusPass
            }
        }
    }
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.log4j2)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.postgresql)
    implementation(libs.liquibase.core)
    implementation(libs.aws.secretsmanager)
    implementation(libs.aws.s3)
    implementation(libs.springdoc.openapi.starter)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockwebserver)
}

ktlint {
    version.set("1.8.0")
    reporters {
        reporter(ReporterType.PLAIN)
    }
}

// ktlint 위반 시 컴파일 실패
tasks.named("compileKotlin") {
    dependsOn("ktlintMainSourceSetCheck")
}

tasks.withType<Test> {
    useJUnitPlatform()
    outputs.dir(snippetsDir)
}

tasks.named("asciidoctor") {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
}
