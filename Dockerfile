# 1
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-Xmx300m", "-Xss512k", "-XX:MaxMetaspaceSize=128m", "-Dserver.port=${PORT:10000}", "-jar", "app.jar"]