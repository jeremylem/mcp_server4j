# Multi-stage build for MCP Server (Java)
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Environment variables with defaults
ENV CHROMA_HOST=chroma
ENV CHROMA_PORT=8000
ENV CHROMA_COLLECTION=baseline_kb

# Expose port for HTTP/SSE transport
EXPOSE 8080

# Use custom entrypoint that can run either ingestion or MCP server
ENTRYPOINT ["docker-entrypoint.sh"]