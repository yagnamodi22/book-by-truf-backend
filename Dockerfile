# Use official OpenJDK 21 image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Build the app (skip tests to speed up)
RUN ./mvnw clean package -DskipTests

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/*.jar"]
