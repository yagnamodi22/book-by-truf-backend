# Use an official Java runtime as a parent image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy project files
COPY src ./src

# Package the application
RUN ./mvnw clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","target/*.jar"]
