FROM eclipse-temurin:17

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8002

# Environment variables can be overridden at runtime
ENV KAFKA_SERVERS=kafka:29092 \
    REDIS_HOST=redis \
    REDIS_PORT=6379

ENTRYPOINT ["java", "-jar", "app.jar", \
            "--spring.kafka.bootstrap-servers=${KAFKA_SERVERS}", \
            "--spring.data.redis.host=${REDIS_HOST}", \
            "--spring.data.redis.port=${REDIS_PORT}"]
