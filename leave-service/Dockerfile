# Multi-stage build for Leave Service
FROM maven:3.8.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml files
COPY leave-service/pom.xml ./leave-service/
COPY auth-service-api/pom.xml ./auth-service-api/

# Download dependencies
RUN mvn dependency:go-offline -f leave-service/pom.xml -B

# Copy source code
COPY leave-service/src ./leave-service/src
COPY auth-service-api/src ./auth-service-api/src

# Build the application
RUN mvn clean package -f leave-service/pom.xml -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads/documents /app/reports

# Copy the built JAR file
COPY --from=build /app/leave-service/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 