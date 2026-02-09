FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="bookmyshow-team"
LABEL service="booking-service"

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
