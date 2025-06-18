# Multi-stage build for Auth Service
FROM maven:3.8.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml files
COPY auth-service/pom.xml ./auth-service/
COPY auth-service-api/pom.xml ./auth-service-api/

# Download dependencies
RUN mvn dependency:go-offline -f auth-service/pom.xml -B

# Copy source code
COPY auth-service/src ./auth-service/src
COPY auth-service-api/src ./auth-service-api/src

# Build the application
RUN mvn clean package -f auth-service/pom.xml -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/auth-service/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 