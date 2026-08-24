FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts .
COPY src src
# 이미지 빌드는 jar 산출만 담당 — 린트/테스트는 CI 게이트에서. ktlint는 헥사고날 `in`
# 패키지 세그먼트(Kotlin 예약어)를 "disallowed character"로 막아 bootJar를 실패시키므로 제외.
RUN chmod +x gradlew && ./gradlew bootJar -x test -x ktlintMainSourceSetCheck -x ktlintTestSourceSetCheck -x ktlintKotlinScriptCheck --no-daemon

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S arc && adduser -S arc -G arc
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER arc
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
