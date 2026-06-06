FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY third-party-service/pom.xml ./third-party-service/pom.xml
RUN mvn -B -q -f third-party-service/pom.xml dependency:go-offline
COPY third-party-service/src ./third-party-service/src
RUN mvn -B -q -f third-party-service/pom.xml clean package -DskipTests
RUN java -Djarmode=layertools -jar third-party-service/target/third-party-service.jar extract --destination third-party-service/target/extracted

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/third-party-service/target/extracted/dependencies/ ./
COPY --from=build /build/third-party-service/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/third-party-service/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/third-party-service/target/extracted/application/ ./
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
