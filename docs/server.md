# Serving analyses: REST API and MCP (`serve`)

`serve` makes the Sokrates analyses in a folder available to tools and AI agents, read-only. It reads the analyses
as they are (each repository's `reports/data/data.zip` and `reports/ai-insights/*.json`, each landscape's
`_sokrates_landscape/config.json`), so there is no database to fill. Run it on the folder you run `analyze`,
`analyzeGitRepo` or `analyzeLandscape` in; later runs are picked up without a restart (new analyses within a minute,
regenerated data on the next request).

```bash
# HTTP: REST under /api/v1, MCP at /mcp, the HTML reports under /reports/
java -jar sokrates.jar serve -root ~/landscapes/acme -port 8080

# on a server or in Docker: listen on every interface, require a token
SOKRATES_SERVER_TOKEN=change-me java -jar sokrates.jar serve -root /data -host 0.0.0.0
docker run --rm -p 8080:8080 -e SOKRATES_SERVER_TOKEN=change-me -v "$(pwd):/code" ghcr.io/zeljkoobrenovic/sokrates serve -host 0.0.0.0

# MCP over stdio, for a local agent
java -jar sokrates.jar serve -root ~/landscapes/acme -mcpStdio
```

| Option | Default | |
| --- | --- | --- |
| `-root` | `.` | the folder with the analyses |
| `-port` | `8080` | `0` picks a free port |
| `-host` | `127.0.0.1` | this machine only; `0.0.0.0` for every interface |
| `-token` | `$SOKRATES_SERVER_TOKEN` | when set, every request needs `Authorization: Bearer <token>` |
| `-mcpStdio` | off | MCP on stdin/stdout instead of HTTP |
| `-hidePeople` | off | pseudonymize contributors and stop serving the HTML reports (see below) |

## People data

Contributors are shown as the analysis identified them: the repository's `ignoreContributors`,
`anonymizeContributors`, `transformContributorEmails` and `config-people.json` are already applied, as in the HTML
reports. By default the API returns their email addresses and user names; protect it with `-token` and the default
`-host 127.0.0.1`.

With **`-hidePeople`** no identity leaves the server, while the people questions still work:

- every contributor becomes `person-<10 hex characters>` (in the `email`/`contributor` fields) and user names are
  dropped. The pseudonym is an HMAC of the email, so one person has the same pseudonym in every repository and every
  answer, and it cannot be turned back into the email. The key is random per process, so pseudonyms change on restart;
  set `SOKRATES_PSEUDONYM_KEY` to keep them stable;
- the contributors `query` matches pseudonyms only, so the server cannot be probed for an address;
- email addresses quoted in AI findings are replaced by their pseudonym;
- the HTML reports are not served (`/reports/` answers 403) and `reportUrl` / finding `url` are left out, because the
  reports list names and emails.

## What is found

- A **repository analysis** is a `data/data.zip` (or, for reports older than the zip, a loose
  `data/analysisResults.json`) outside any `_sokrates_landscape` folder. Its **id** is the path of its analysis folder
  relative to the root (`acme/payments`, `_sokrates`); its **name** is the configured `metadata.name` (usually
  `owner/repo`). Anywhere a repository is asked for, either works (the name case-insensitively).
- A **landscape** is a `_sokrates_landscape/config.json` (the generated virtual landscapes are not listed). Its id is
  the folder holding `_sokrates_landscape`; its repositories are the analyses below that folder.

## REST API (`/api/v1`)

| Request | Returns |
| --- | --- |
| `GET /api/v1` | the list of endpoints |
| `GET /api/v1/landscapes` | landscapes with repository counts |
| `GET /api/v1/overview?landscape=` | a landscape (or, without `landscape`, every analysis) at a glance: repository counts (with code, active in 30 / 365 days), lines of code per scope and main language, ease-of-change grade distributions and size-weighted averages, duplication, commits and distinct contributors (people merged as the landscape merges them, bots apart, counts only), AI findings per severity, and the largest, most active and lowest-scoring repositories. Grades and the lowest-scoring list only count repositories with at least 1,000 main lines |
| `GET /api/v1/repositories?query=&landscape=&sort=&limit=&offset=` | a page of repositories: main language, lines of code, ease-of-change scores, 30-day commits and contributors, AI findings count, report link |
| `GET /api/v1/repository?name=` | one repository: size per scope, languages, duplication, file size / unit size / unit complexity risk profiles, scores with sub-scores, activity, components |
| `GET /api/v1/repository/hotspots?name=&window=365\|90\|all&limit=` | the main files to look at first, ranked as the report's Highlights tab: complexity × days changed in the past 365 days (`window=90`: commits in 90 days; `window=all`: days changed, all time, also used when nothing changed in the window). Analyses made before the `# active days (365d)` column existed fall back to the 90-day commits until they are regenerated |
| `GET /api/v1/repository/contributors?name=&query=&includeBots=&limit=&offset=` | contributors with commits in the past 30/90/365 days and all time, most active in the past year first (bots only with `includeBots=true`) |
| `GET /api/v1/repository/experts?name=&path=&component=&decomposition=&languages=&limit=` | who knows a part of one repository's main code — a file or folder (`path`, a prefix), a logical component (`component`: a name or `decomposition::component`) and/or languages (`languages`: file extensions, e.g. `tf` or `java,kt`): the people who changed it, most active in the past 365 days first, and the top person's share of the past year's changes (a knowledge risk when high); needs an analysis made after 2026-11-25 |
| `GET /api/v1/experts?landscape=&path=&component=&decomposition=&languages=&limit=` | the same across the repositories of a landscape (people merged by its `ignoreContributors`, `transformContributorEmails` and `config-people.json`, as in its report) or, without `landscape`, of every analysis (identities as analysed); adds each person's repository count and top repositories, and `repositoriesNotCovered` (analyses too old to say) |
| `GET /api/v1/repository/dependencies?name=&decomposition=&component=` | the dependencies between the components of a decomposition (the first by default), with cycles marked |
| `GET /api/v1/findings?repository=&landscape=&query=&minSeverity=&scanner=&limit=&offset=` | the AI findings of the sokrates-skills scanners, most severe first |

`query` uses the landscape repository list's syntax: terms split by `;`, `mainLang:java,kt`, `includesLang:tf`, any
other term matches the name or description. `sort`: `loc` (default), `name`, `commits`, `humanScore`, `aiScore`
(lowest first), `findings`. Lists are pages (`total`, `offset`, `items`; at most 500 items). Numbers are those of the
analysis: the 30/90-day windows end at the analysis date, not at the time of the request.

`reportUrl` and finding `url` values are paths on the server (`/reports/…`). Only files inside a repository's
`reports` folder or a landscape folder are served, never other files below the root, such as sources.

## MCP

Tools: `list_landscapes`, `get_landscape_overview` (`/api/v1/overview`), `list_repositories`, `get_repository`, `get_hotspots`, `get_contributors`, `who_knows`,
`get_dependencies`, `search_findings`, with the same parameters as the REST requests (`who_knows` without
`repository` is `/api/v1/experts`). Over HTTP the endpoint is `POST /mcp` (Streamable HTTP, JSON responses, no
server-initiated streams; a browser `Origin` must match the `Host`). A local agent can start it over stdio, e.g. in
Claude Code's `.mcp.json`:

```json
{
  "mcpServers": {
    "sokrates": {
      "command": "java",
      "args": ["-jar", "/path/to/cli-1.0-jar-with-dependencies.jar", "serve", "-root", "/path/to/analyses", "-mcpStdio"]
    }
  }
}
```

or, against a running server: `claude mcp add --transport http sokrates http://localhost:8080/mcp`
(with a token: `--header "Authorization: Bearer <token>"`).
