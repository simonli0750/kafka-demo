FROM eclipse-temurin:17

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8001

# Environment variables can be overridden at runtime
ENV KAFKA_SERVERS=kafka:29092

ENTRYPOINT ["java", "-jar", "app.jar", \
            "--spring.kafka.bootstrap-servers=${KAFKA_SERVERS}"]
