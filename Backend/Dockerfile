FROM --platform=$BUILDPLATFORM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build --chown=10001:10001 /app/target/quarkus-app/ ./
COPY --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh
USER 10001
EXPOSE 8082
ENTRYPOINT ["/app/docker-entrypoint.sh"]
