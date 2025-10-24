# Use OpenJDK 21 for modern Java features
FROM openjdk:21-jdk-slim

# Set working directory inside container
WORKDIR /app

# Copy project files
COPY . .

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Build your Spring Boot app
RUN ./mvnw clean package -DskipTests

# Find the JAR file (since the name changes after build)
RUN mv target/*.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
