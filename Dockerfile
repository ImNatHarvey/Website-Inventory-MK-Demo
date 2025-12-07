# Stage 1: Build the application using a Maven image
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy Maven wrapper, pom.xml, and the .mvn folder for dependency caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src src

# Package the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the final lean runtime image
FROM eclipse-temurin:17-jre-alpine

# Expose the application port
EXPOSE 8080

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR file from the build stage
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

# Define the entry point for the container
ENTRYPOINT ["java", "-jar", "/app.jar"]