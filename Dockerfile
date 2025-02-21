# Use OpenJDK as the base image
FROM maven:3.8.5-openjdk-21 AS build
COPY . .
RUN mvn clean package -DskipTests
FROM openjdk:21-jdk-slim
COPY --from=build /target/aids-0.0.1-SNAPSHOT.jar aids.jar
EXPOSE 8080
CMD ["java", "-jar", "aids.jar"]



