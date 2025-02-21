# Use Maven with JDK 21 for building
FROM maven:3.9.0-eclipse-temurin-21-jdk AS build
COPY . .
RUN mvn clean package -DskipTests

# Use OpenJDK 21 for running the app
FROM eclipse-temurin:21-jdk
COPY --from=build /target/aids-0.0.1-SNAPSHOT.jar aids.jar
EXPOSE 8080
CMD ["java", "-jar", "aids.jar"]
