# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage for Eureka Server
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks and clean up
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create a non-root user
RUN useradd -m -s /bin/bash appuser \
    && chown -R appuser /app

# Switch to non-root user
USER appuser

# Copy the pre-built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 