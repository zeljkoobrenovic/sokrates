# Sokrates code scanner with Kitnetic wrapper

The Kitnetic wrapper is an interactive CLI tool that orchestrates running the **Sokrates** code scanner across one or more selected project folders.

The wrapper simplifies project selection, execution, and report collection.

Source code: https://github.com/kitnetic/sokrates (fork of the official Sokrates repository)

---

## Sokrates

Know your code! The unexamined code is not worth maintaining!

For details and examples visit the website [sokrates.dev](https://sokrates.dev).

Sokrates is built by Željko Obrenović. It implements his "examined code" vision on how to approach understanding of complex source code bases, in a pragmatic and efficient way.

---

## Kitnetic Wrapper Overview

- Runs the Sokrates scanner against one or more projects
- Interactive terminal-based project selection
- Designed to be run via Docker
- Consolidates generated Sokrates reports into a single output directory
- Can be run fully offline (air-gapped)

---

## Running the scanner

The tool is intended to be run via Docker. You may either:

- build the Docker image locally, or
- pull a pre-built image from Docker Hub:

👉 https://hub.docker.com/repository/docker/kitnetic/kitnetic-sokrates

When the container starts, you will be prompted via an interactive CLI to select which projects to scan.

Each selected project will then be scanned sequentially, and Sokrates report files will be written to the output directory.

---

## Example usage

```
docker run -it \
  -v "$PWD:/input" \
  -v "$PWD/output:/output" \
  kitnetic/kitnetic-sokrates
```

## Volume mounts

Two volumes must be mounted when running the container:

| **Container Path** | **Purpose**                                                                 |
|--------------------|-----------------------------------------------------------------------------|
| /input             | Directory containing one or more source code projects, each as a sub-folder |
| /output            | Directory where Sokrates report files will be written                       |


Example input layout:

```
/input
├── project-a
├── project-b
└── project-c
```

Each sub-folder will be presented as a selectable project in the interactive CLI.

## Output

The scanner generates one report per project in an HTML format.

All reports are written to `/output`, and stored in a folder named `_kitnetic_sokrates_reports`.

## Running in an air-gapped environment

The scanner can be run without any network connectivity.

Recommended steps:
1. Build or pull the Docker image
1. Disconnect the host machine from the internet
1. Run the Docker container using the standard command
1. Reconnect to the internet after the scan completes
1. No network access is required during execution.

## Important notes

* The Docker container **requires write access** to:
  * `/input` (for temporary scan artifacts)
  * `/output` (for generated reports)
* The wrapper **requires an interactive TTY**:
  * Always run Docker with the `-it` flags 
* Scans are performed sequentially, one project at a time