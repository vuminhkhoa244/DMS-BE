FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 -S sgroup && \
    adduser -u 1001 -S suser -G sgroup

WORKDIR /app

COPY --from=build --chown=1001:1001 /app/target/*.jar app.jar

USER suser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
