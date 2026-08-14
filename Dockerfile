# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the POM first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy the rest of the source and build
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Create a non-root user to run the app
RUN useradd -ms /bin/bash spring
USER spring

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Persist H2 file-based database outside the container layer
VOLUME ["/app/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]