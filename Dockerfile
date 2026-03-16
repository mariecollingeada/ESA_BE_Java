# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy Maven wrapper and pom first (layer-cached until pom changes)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build the JAR, skipping tests (tests run in CI separately)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: runtime ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the jar built in the build stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Copy entrypoint and give execute permissions
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Informational (helps some platforms detect the service)
EXPOSE 8080

# Use our entrypoint script that builds SPRING_* env vars then execs the jar
ENTRYPOINT ["/app/entrypoint.sh"]