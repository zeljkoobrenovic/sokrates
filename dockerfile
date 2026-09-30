# syntax=docker/dockerfile:1

# Stage 1: build the CLI fat jar.
# --platform=$BUILDPLATFORM runs Maven natively on the build machine even for a multi-arch build
# (linux/amd64 + linux/arm64); the jar is platform-independent, so only the runtime stage below
# is per-architecture. Without it, the arm64 image would compile the whole project under QEMU.
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .

# Build the project with Maven (tests run in CI, not in the image build).
RUN --mount=type=cache,target=/root/.m2 mvn -q clean install -DskipTests

# Stage 2: the runtime image — a plain JRE. Git history is extracted with JGit, and graphs are
# rendered in the browser (Mermaid.js/d3), so no git, Graphviz or other native tooling is needed.
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="Sokrates" \
      org.opencontainers.image.description="Source code analysis: scans a code base and generates HTML reports (size, duplication, structure, dependencies, contributors, trends)" \
      org.opencontainers.image.url="https://sokrates.dev" \
      org.opencontainers.image.source="https://github.com/zeljkoobrenovic/sokrates" \
      org.opencontainers.image.licenses="MIT"

# Copy the Sokrates CLI jar from the build stage
COPY --from=build /build/cli/target/cli-1.0-jar-with-dependencies.jar /app/sokrates-cli.jar

# The code base to analyze is mounted here: docker run -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates analyze
WORKDIR /code

# Define the entry point for the container
ENTRYPOINT ["java", "-jar", "/app/sokrates-cli.jar"]
CMD ["analyze"]
