# Dockerfile (copy-paste)

# ── Stage 1: build using official Maven image (JDK 21) ───────────────
FROM maven:3.9.13-eclipse-temurin-21 AS build

WORKDIR /app

# Copy project files
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src

# Build the jar (skip tests to speed up build)
RUN mvn -B -DskipTests package

# ── Stage 2: runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install python3 for the entrypoint conversion logic (and CA certs)
RUN apt-get update && apt-get install -y python3 ca-certificates --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Copy the built artifact from the build stage (adjust name if your artifact differs)
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Copy entrypoint (make sure entrypoint.sh is in repo root)
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]