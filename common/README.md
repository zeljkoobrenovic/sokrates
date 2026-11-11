# common

Foundation module for Sokrates. It contains general-purpose utilities with **no Sokrates-specific domain logic**, so it sits at the bottom of the dependency chain (every other module depends on it).

Package root: `nl.obren.sokrates.common`.

## What's here

* **io** — JSON serialization/deserialization helpers (`JsonGenerator`, `JsonMapper`, built on Jackson via Jersey) and user-properties IO.
* **renderingutils** — rich-text/HTML rendering helpers, plus chart and graph rendering support:
  * `charts` — chart rendering helpers
  * `force3d` / `x3d` — 3D force-graph and X3D visualization rendering
* **analysis** — small shared analysis primitives (e.g. `Finding`).
* **utils** — formatting, reflection, and system utilities.

## Build / test

```bash
mvn -pl common -am install      # build this module
mvn -pl common test             # run its tests
```
