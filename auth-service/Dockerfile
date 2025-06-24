# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy the entire project context to access auth-service-api
COPY . .

# Build the auth-service-api first, then auth-service
RUN cd auth-service-api && mvn clean install -DskipTests
RUN cd auth-service && mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root user
RUN useradd -m -s /bin/bash appuser
USER appuser

# Copy JAR from build stage
COPY --from=build /app/auth-service/target/*.jar app.jar

# Expose port and health check
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"] 