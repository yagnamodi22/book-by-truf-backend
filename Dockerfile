# Use OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and project files
COPY . .

# Grant execute permission for mvnw
RUN chmod +x mvnw

# Build the project (skip tests to speed up)
RUN ./mvnw clean package -DskipTests

# Expose the port your app runs on
EXPOSE 8080

# Run the built jar file
CMD ["java", "-jar", "target/*.jar"]
