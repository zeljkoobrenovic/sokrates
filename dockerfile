# -------- Stage 1: Build Sokrates --------
FROM maven:3.9.9-eclipse-temurin-17 AS sokrates_builder

WORKDIR /build
COPY . .

RUN mvn clean install -DskipTests

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
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install Graphviz (required for running Sokrates)
RUN apt-get update && \
    apt-get install -y graphviz && \
    rm -rf /var/lib/apt/lists/*

ENV GRAPHVIZ_DOT="/usr/bin/dot"

# Copy the Sokrates CLI jar from the build stage
COPY --from=sokrates_builder /build/cli/target/cli-1.0-jar-with-dependencies.jar sokrates-cli.jar

# Copy the Kitnetic wrapper executable from the build stage
COPY --from=kitnetic_builder /kitnetic_wrapper/dist/kitnetic-wrapper /usr/local/bin/kitnetic-wrapper

# Set environment variables for the Kitnetic wrapper
ENV SOKRATES_JAR_FILE_PATH=/app/sokrates-cli.jar

# Run as non-root user
RUN useradd -m appuser
USER appuser

# Define the Kitentic wrapper as the entry point for the container
ENTRYPOINT ["/usr/local/bin/kitnetic-wrapper"]