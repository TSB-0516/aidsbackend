# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built JAR file from target directory to the container
COPY target/aids-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on (default 8080)
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]
