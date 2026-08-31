# --- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so `docker compose build` is fast on rebuilds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/oauth.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
