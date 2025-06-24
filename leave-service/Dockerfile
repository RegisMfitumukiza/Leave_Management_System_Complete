# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy all sources
COPY . .

# Build and install shared module
RUN cd auth-service-api && mvn clean install -DskipTests

# Build the service
RUN cd leave-service && mvn clean package -DskipTests

# Runtime stage for Leave Service
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN useradd -m -s /bin/bash appuser
USER appuser
RUN mkdir -p /app/uploads/documents /app/reports
COPY --from=build /app/leave-service/target/*.jar app.jar
VOLUME ["/app/uploads", "/app/reports"]
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"] 