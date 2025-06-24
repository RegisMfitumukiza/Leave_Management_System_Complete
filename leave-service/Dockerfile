# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage for Leave Service
FROM eclipse-temurin:17-jre-jammy

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Create uploads and reports directories
RUN mkdir -p /app/uploads/documents /app/reports

# Create a non-root user (same as in auth-service)
RUN useradd -m -s /bin/bash appuser \
    && chown -R appuser /app

# Set permissions for appuser
RUN chmod -R 755 /app/uploads /app/reports

# Switch to non-root user
USER appuser

# Copy the pre-built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Volume declarations for persistent storage
VOLUME ["/app/uploads", "/app/reports"] 