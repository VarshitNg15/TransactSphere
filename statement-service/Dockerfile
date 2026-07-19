FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/statement-service-1.0.0.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
