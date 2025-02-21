# Use Maven with JDK 21 for building
FROM maven:3.9.0-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Use JDK 21 for running the application
FROM eclipse-temurin:21-jdk
COPY --from=build /target/aids-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
