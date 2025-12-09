# Build stage
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /workspace
# Copy maven wrapper & pom (context is root, so use backend/ prefix)
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn ./.mvn
# Copy source code
COPY backend/src ./src
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests package -DskipDockerBuild=true

#run stage
FROM eclipse-temurin:24-jre-alpine
ARG APP_JAR=target/*.jar
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

# Create non-root user for safety
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8089
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8089/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
