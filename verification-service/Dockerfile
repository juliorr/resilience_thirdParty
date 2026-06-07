FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY verification-service/pom.xml ./verification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f verification-service/pom.xml dependency:go-offline
COPY verification-service/src ./verification-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f verification-service/pom.xml clean package -DskipTests
RUN java -Djarmode=layertools -jar verification-service/target/verification-service.jar extract --destination verification-service/target/extracted

FROM eclipse-temurin:21-jre AS runtime
LABEL org.opencontainers.image.title="verification-service" \
      org.opencontainers.image.source="https://github.com/juliorr/incode"
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
COPY --from=build /build/verification-service/target/extracted/dependencies/ ./
COPY --from=build /build/verification-service/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/verification-service/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/verification-service/target/extracted/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
