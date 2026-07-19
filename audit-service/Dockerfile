FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/audit-service-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
