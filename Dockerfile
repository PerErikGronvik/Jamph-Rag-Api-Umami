# Multi-stage build for Jamph-Rag-Api-Umami
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY api/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY api/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install system packages for healthcheck and networking
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    wget \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy the fat JAR from builder stage
COPY --from=builder /build/target/api-1.0-SNAPSHOT-jar-with-dependencies.jar ./app.jar

# Expose API port
EXPOSE 8004

# Environment variables with defaults
ENV API_PORT=8004 \
    API_HOST=0.0.0.0 \
    OLLAMA_BASE_URL=http://localhost:11434 \
    OLLAMA_MODEL=llama3.2:3b

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
