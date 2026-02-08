# Use Eclipse Temurin OpenJDK 21 as base image
FROM eclipse-temurin:21-jdk-jammy

# Set working directory
WORKDIR /app

# Install Maven directly
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for better caching
COPY pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn package -DskipTests

# Create a new stage for the runtime
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built jar from the previous stage
COPY --from=0 /app/target/*.jar app.jar

# Change ownership of the app directory to appuser
RUN chown -R appuser:appuser /app

# Switch to the non-root user
USER appuser

# Expose the port your app runs on
EXPOSE 8088

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
