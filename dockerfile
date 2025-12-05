# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .

# Build the project with Maven
RUN mvn clean install -DskipTests

# Stage 2: Setup the runtime environment
FROM eclipse-temurin:17-jre

# Install Graphviz (required for running Sokrates)
RUN apt-get update && \
    apt-get install -y graphviz && \
    rm -rf /var/lib/apt/lists/*

# Set the work directory
WORKDIR /app

# Copy the Sokrates CLI jar from the build stage
COPY --from=build /build/cli/target/cli-1.0-jar-with-dependencies.jar sokrates-cli.jar

# Set the environment variable for Graphviz, if necessary
ENV GRAPHVIZ_DOT="/usr/bin/dot"

# Define the entry point for the container
ENTRYPOINT ["java", "-jar", "/app/sokrates-cli.jar"]