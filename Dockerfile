# Multi-stage Dockerfile for Clockwork Scheduler
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y postgresql-client curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/clockwork-scheduler-*.jar app.jar

EXPOSE 8080 8000
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
