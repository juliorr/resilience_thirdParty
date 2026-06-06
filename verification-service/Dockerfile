FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY verification-service/pom.xml ./verification-service/pom.xml
RUN mvn -B -q -f verification-service/pom.xml dependency:go-offline
COPY verification-service/src ./verification-service/src
RUN mvn -B -q -f verification-service/pom.xml clean package -DskipTests
RUN java -Djarmode=layertools -jar verification-service/target/verification-service.jar extract --destination verification-service/target/extracted

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/verification-service/target/extracted/dependencies/ ./
COPY --from=build /build/verification-service/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/verification-service/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/verification-service/target/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
