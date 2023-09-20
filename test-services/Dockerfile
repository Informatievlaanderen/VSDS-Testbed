# Stage 1: Build application
FROM maven:3.9.2-amazoncorretto-17 AS builder

WORKDIR /app
COPY . /app
RUN mvn clean install -DskipTests=true

# Stage 2: Run application
FROM eclipse-temurin:17-jre-alpine

RUN mkdir /app
COPY --from=builder /app/target/test-services.jar /app/app.jar
RUN sh -c 'touch /app/app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx4096m","-jar","/app/app.jar"]
EXPOSE 8080
WORKDIR /app