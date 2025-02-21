# Use OpenJDK as the base image
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvnw clean package -DskipTests
FROM openjdk:17-jdk-slim
COPY --from=build /target/aids-0.0.1-SNAPSHOT.jar aids.jar
EXPOSE 8080
CMD ["java", "-jar", "aids.jar"]

# Set the working directory inside the container

# Copy Maven wrapper and source code


# Build the JAR file


# Copy the built JAR file to the container


# Expose the port your Spring Boot app runs on


# Run the application

