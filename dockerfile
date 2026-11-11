# syntax=docker/dockerfile:1

# -------- Stage 1: Build Sokrates --------
# --platform=$BUILDPLATFORM runs Maven natively on the build machine even for a multi-arch build;
# the jar is platform-independent, so only the later stages are per-architecture.
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17 AS sokrates_builder

WORKDIR /build
COPY . .

# Build the project with Maven (tests run in CI, not in the image build).
RUN --mount=type=cache,target=/root/.m2 mvn -q clean install -DskipTests

# -------- Stage 2: Build Kitnetic wrapper --------
FROM python:3.12-slim AS kitnetic_builder

WORKDIR /kitnetic_wrapper

RUN apt-get update && apt-get install -y --no-install-recommends build-essential \
  && rm -rf /var/lib/apt/lists/*

COPY kitnetic-wrapper/pyproject.toml kitnetic-wrapper/poetry.lock kitnetic-wrapper/README.md .
COPY kitnetic-wrapper/src/ src/

RUN pip install --no-cache-dir poetry==1.8.3 pyinstaller
RUN poetry export -f requirements.txt --output requirements.txt --without-hashes
RUN pip install --no-cache-dir -r requirements.txt
RUN pip install --no-cache-dir .

RUN pyinstaller --onefile --name kitnetic-wrapper /kitnetic_wrapper/src/kitnetic_wrapper/cli.py

# -------- Stage 3: Setup runtime environment --------
# A plain JRE: Sokrates extracts git history with JGit and renders graphs in the browser
# (Mermaid.js/d3), so no git, Graphviz or other native tooling is needed.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the Sokrates CLI jar from the build stage
COPY --from=sokrates_builder /build/cli/target/cli-1.0-jar-with-dependencies.jar sokrates-cli.jar

# Copy the Kitnetic wrapper executable from the build stage
COPY --from=kitnetic_builder /kitnetic_wrapper/dist/kitnetic-wrapper /usr/local/bin/kitnetic-wrapper

# Set environment variables for the Kitnetic wrapper
ENV SOKRATES_JAR_FILE_PATH=/app/sokrates-cli.jar

# Let the JVM use most of the container's memory (its default is 25%, which runs out on big
# repositories). The wrapper invokes `java -jar` itself, so this is set via JAVA_TOOL_OPTIONS
# rather than the entrypoint. An explicit -e JAVA_TOOL_OPTIONS=-Xmx8g still overrides it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Run as non-root user
RUN useradd -m appuser
USER appuser

# Define the Kitnetic wrapper as the entry point for the container
ENTRYPOINT ["/usr/local/bin/kitnetic-wrapper"]
