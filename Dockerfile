FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S arc && adduser -S arc -G arc
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER arc
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
