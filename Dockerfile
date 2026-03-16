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

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
