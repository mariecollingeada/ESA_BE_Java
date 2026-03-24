FROM maven:3.9.13-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y python3 ca-certificates --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]