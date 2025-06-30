FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080 8583
ENTRYPOINT ["java", "-jar", "app.jar"]