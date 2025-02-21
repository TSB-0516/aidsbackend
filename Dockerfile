# Use OpenJDK as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy Maven wrapper and source code
COPY . .

# Build the JAR file
RUN ./mvnw clean package -DskipTests

# Copy the built JAR file to the container
COPY ./target/aids-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
