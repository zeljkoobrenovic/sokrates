# cli

The command line interface for Sokrates and the git-history extraction utilities. This module's fat jar (`cli/target/cli-1.0-jar-with-dependencies.jar`) is the primary way to run Sokrates.

Package root: `nl.obren.sokrates.cli`. Depends on [`common`](../common/README.md), [`codeanalyzer`](../codeanalyzer/README.md), and [`reports`](../reports/README.md).

Main class: `nl.obren.sokrates.cli.CommandLineInterface`.

## What's here

* `CommandLineInterface` — entry point; parses arguments and dispatches commands.
* `Commands` — the catalog of commands and options (Apache Commons CLI). The single source of truth for command names, descriptions, and flags.
* `CommandUsage` — usage/help text rendering.
* `git/GitHistoryExtractor` — extracts git history into the `git-history.txt` format consumed by history/contributor analyses.

## Commands

See the [root README](../README.md#commands) for the command table and the **init → generateReports** workflow. To see options for any command:

```bash
java -jar cli-1.0-jar-with-dependencies.jar <command> -help
```

Defaults: config is read from `<currentFolder>/_sokrates/config.json`; reports are written to `<currentFolder>/_sokrates/reports/`.

## Build / test

```bash
mvn -pl cli -am install
mvn -pl cli test
```
