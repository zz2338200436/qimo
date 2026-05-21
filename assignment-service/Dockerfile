FROM maven:3.9-eclipse-temurin-17 AS builder

ARG MODULE_NAME=assignment-service
ARG JAR_NAME=assignment-service-0.1.0-SNAPSHOT.jar

WORKDIR /workspace
COPY . .
RUN mvn --batch-mode -pl ${MODULE_NAME} -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /workspace/${MODULE_NAME}/target/${JAR_NAME} /app/app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
