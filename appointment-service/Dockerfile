FROM ubuntu:latest
LABEL authors="indiw"
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/appointment-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
ENTRYPOINT ["top", "-b"]