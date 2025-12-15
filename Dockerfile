# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /app/target/keycloak-sms-otp.jar /opt/keycloak/providers/
