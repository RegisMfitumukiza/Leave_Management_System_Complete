# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy all sources
COPY . .

# Build and install shared module
RUN cd auth-service-api && mvn clean install -DskipTests

# Build the service
RUN cd auth-service && mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health checks and clean up
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -s /bin/bash appuser
USER appuser

# Copy JAR from build stage
COPY --from=build /app/auth-service/target/*.jar app.jar

# Expose port and health check
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"] 