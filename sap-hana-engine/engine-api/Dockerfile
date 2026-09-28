# Multi-stage Dockerfile for SAP HANA Engine API (Java 21 LTS)

# Stage 1: Build JAR with Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & POMs
COPY pom.xml .
COPY engine-core/pom.xml engine-core/
COPY engine-connector-jdbc/pom.xml engine-connector-jdbc/
COPY engine-connector-rest/pom.xml engine-connector-rest/
COPY engine-persistence/pom.xml engine-persistence/
COPY engine-ingestion/pom.xml engine-ingestion/
COPY engine-analysis/pom.xml engine-analysis/
COPY engine-api/pom.xml engine-api/

# Copy source code
COPY engine-core/src engine-core/src
COPY engine-connector-jdbc/src engine-connector-jdbc/src
COPY engine-connector-rest/src engine-connector-rest/src
COPY engine-persistence/src engine-persistence/src
COPY engine-ingestion/src engine-ingestion/src
COPY engine-analysis/src engine-analysis/src
COPY engine-api/src engine-api/src

# Build production fat JAR
RUN ./mvnw clean package -DskipTests -f pom.xml || true

# Fallback: compile if mvnw wrapper omitted
FROM maven:3.9-eclipse-temurin-21-alpine AS maven-builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -f pom.xml

# Stage 2: Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
EXPOSE 8080

COPY --from=maven-builder /app/engine-api/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
