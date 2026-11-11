from __future__ import annotations

import os
import shutil
import json
import subprocess
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Dict

import typer
from InquirerPy import inquirer
from InquirerPy.base.control import Choice
from rich.console import Console
from bs4 import BeautifulSoup

app = typer.Typer(add_completion=False, help="Run a Sokrates code scan on one or more projects.")
console = Console()

SOKRATES_JAR_FILE_PATH = os.environ.get("SOKRATES_JAR_FILE_PATH", "/app/sokrates-cli.jar")

@dataclass
class Project:
    folder_path: Path
    name: str
    description: str

@dataclass
class SokratesRunResult:
    folder_path: Path
    command: List[str]
    return_code: int
    started_at: str
    finished_at: str

@dataclass
class ScanProjectResult:
    project: Project
    is_success: bool
    started_at: str
    finished_at: str

def _get_now_as_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

def format_scan_duration(start_iso: str, end_iso: str) -> str:
    start = datetime.fromisoformat(start_iso)
    end = datetime.fromisoformat(end_iso)

    total_seconds = int((end - start).total_seconds())
    minutes, seconds = divmod(total_seconds, 60)

    return f"{minutes}m {seconds}s"

def is_execution_interactive() -> bool:
    return sys.stdin.isatty() and sys.stdout.isatty()

def list_immediate_subfolders(root: Path) -> List[Path]:
    if not root.exists():
        raise FileNotFoundError(f"Root folder does not exist: {root}")
    if not root.is_dir():
        raise NotADirectoryError(f"Root path is not a directory: {root}")

    subfolders: List[Path] = []
    for entry in sorted(root.iterdir(), key=lambda p: p.name.lower()):
        if entry.is_dir():
            subfolders.append(entry)
    return subfolders

def prompt_for_project_selection(subfolders: List[Path]) -> List[Path]:
    choices = [Choice(value=p, name=p.name) for p in subfolders]
    selected: List[Path] = inquirer.checkbox(
        message="Select one or more projects to scan:",
        choices=choices,
        instruction="(Space to select, Enter to confirm)",
        cycle=True,
        validate=lambda result: len(result) > 0 or "Select at least one folder.",
    ).execute()
    return selected

def prompt_for_project_metadata(folder_path: Path) -> Project:
    console.print(f"\n[bold]Please provide a name and description for '{folder_path}'[/bold]")

    name: str = inquirer.text(
        message="Name:",
        default=folder_path.name,
        validate=lambda s: (len(s.strip()) > 0) or "Name cannot be empty.",
    ).execute()

    description: str = inquirer.text(
        message="Description:",
        default="",
        validate=lambda s: (len(s.strip()) > 0) or "Description cannot be empty.",
    ).execute()

    return Project(folder_path=folder_path, name=name.strip(), description=description.strip())

def build_sokrates_command(tool_params: List[str]) -> List[str]:
    command = [
        "java",
        "-jar",
        str(SOKRATES_JAR_FILE_PATH),
    ]

    command.extend(tool_params)

    return command

def run_sokrates(
        path: Path,
        tool_params: List[str],
        dry_run: bool
) -> SokratesRunResult:
    cmd = build_sokrates_command(tool_params)

    started = _get_now_as_iso()

    if dry_run:
        finished = _get_now_as_iso()
        return SokratesRunResult(
            folder_path=path,
            command=cmd,
            return_code=0,
            started_at=started,
            finished_at=finished,
        )

    completed_process = subprocess.run(
        cmd,
        text=True,
        capture_output=True,
        check=False,
        cwd=path,
    )
    finished = _get_now_as_iso()

    return SokratesRunResult(
        folder_path=path,
        command=cmd,
        return_code=completed_process.returncode,
        started_at=started,
        finished_at=finished,
    )

# Files Sokrates' extractGitHistory writes into the scanned project folder. git-commits.txt holds
# commit subject lines and git-commit-trailers.txt holds trailers such as co-author names, so none
# of them should be left behind in the vendor's source tree.
GIT_HISTORY_ARTEFACT_FILE_NAMES = ("git-history.txt", "git-commits.txt", "git-commit-trailers.txt")

def disable_source_code_export(project: Project, dry_run: bool) -> None:
    """Stop Sokrates copying source files and code snippets into its reports.

    `updateConfig -setCacheFiles false` only covers saveSourceFiles, so both flags are set by
    editing the config that `init` wrote. With both off, Sokrates embeds no code in
    reports/src/viewer.html and leaves the "view code" links out of the report pages.
    """
    if dry_run:
        return

    config_file_path = project.folder_path / "_sokrates" / "config.json"
    config = json.loads(config_file_path.read_text(encoding="utf-8"))

    analysis = config.setdefault("analysis", {})
    analysis["saveSourceFiles"] = False
    analysis["saveCodeFragments"] = False

    config_file_path.write_text(json.dumps(config, indent=2), encoding="utf-8")

def cleanup_project_scan_artefacts(project: Project, dry_run: bool) -> None:
    if dry_run:
        return

    for file_name in GIT_HISTORY_ARTEFACT_FILE_NAMES:
        (project.folder_path / file_name).unlink(missing_ok=True)

def scan_project(project: Project, dry_run: bool) -> ScanProjectResult:
    started = _get_now_as_iso()

    def build_result(success: bool) -> ScanProjectResult:
        return ScanProjectResult(
            project=project,
            is_success=success,
            started_at=started,
            finished_at=_get_now_as_iso()
        )

    if dry_run:
        return build_result(True)

    try:
        console.print(f"[cyan]Stage 1 of 5:[/cyan] Extracting Git history")
        result = run_sokrates(project.folder_path, ["extractGitHistory"], dry_run=dry_run)
        if result.return_code != 0:
            return build_result(False)

        console.print(f"[cyan]Stage 2 of 5:[/cyan] Running Sokrates scan")
        result = run_sokrates(
            project.folder_path,
            ["init", "-name", project.name, "-description", project.description],
            dry_run=dry_run
        )
        if result.return_code != 0:
            return build_result(False)

        console.print(f"[cyan]Stage 3 of 5:[/cyan] Disabling source code export")
        try:
            disable_source_code_export(project, dry_run=dry_run)
        except (OSError, ValueError) as e:
            # Never generate reports if the source code export could not be switched off.
            console.print(f"[red]Could not update the Sokrates configuration:[/red] {e}")
            return build_result(False)

        console.print(f"[cyan]Stage 4 of 5:[/cyan] Generating Sokrates scan report")
        result = run_sokrates(project.folder_path, ["generateReports"], dry_run=dry_run)
        if result.return_code != 0:
            return build_result(False)

        return build_result(True)
    finally:
        # Runs on failure too, so git history files are never left in the vendor's source tree.
        console.print(f"[cyan]Stage 5 of 5:[/cyan] Cleaning up Sokrates scan artefacts")
        cleanup_project_scan_artefacts(project, dry_run=dry_run)

def scan_projects(projects: List[Project], dry_run: bool) -> List[ScanProjectResult]:
    console.print("\n[bold]Starting scans...[/bold]")
    results: List[ScanProjectResult] = []
    for idx, project in enumerate(projects, start=1):
        console.print(f"\n[bold]({idx}/{len(projects)})[/bold] Scanning [green]{project.name}[/green]")

        result = scan_project(project, dry_run)
        results.append(result)

        scan_duration = format_scan_duration(result.started_at, result.finished_at)

        if result.is_success:
            console.print(f"[green]Scan completed successfully in {scan_duration}[/green]")
        else:
            console.print(f"[red]Scan failed after {scan_duration}[/red]")

    return results

def consolidate_sokrates_reports(output_folder_path: Path, scan_results: List[ScanProjectResult]) -> None:
    if output_folder_path.exists():
        shutil.rmtree(output_folder_path)

    output_folder_path.mkdir(parents=True, exist_ok=True)

    for result in scan_results:
        source_path  = result.project.folder_path.resolve() / "_sokrates"
        if not source_path.exists() or not source_path.is_dir():
            console.print(f"\n[yellow]Sokrates report folder for {source_path} could not be found[/yellow]]")
            continue

        destination_path = output_folder_path.resolve() / result.project.folder_path.name

        shutil.move(str(source_path), str(destination_path))

def write_summary_report(output_folder_path: Path, results: List[ScanProjectResult]) -> None:
    scans: List[dict] = []
    for result in results:
        scans.append({
            "name": result.project.name,
            "description": result.project.description,
            "folder_name": result.project.folder_path.name,
            "is_success": result.is_success,
            "started_at": result.started_at,
            "finished_at": result.finished_at,
        })

    report = {
        "generated_at": _get_now_as_iso(),
        "scans": scans,
        "summary": {
            "total_scans": len(results),
            "succeeded_scans": sum(1 for r in results if r.is_success),
            "failed_scans": sum(1 for r in results if r.is_success == False),
        },
    }

    path = output_folder_path.resolve() / "kitnetic_summary_report.json"
    path.write_text(json.dumps(report, indent=2), encoding="utf-8")

def sanitise_html_reports(output_folder_path: Path) -> None:
    project_folder_paths = list_immediate_subfolders(output_folder_path)
    for folder_path in project_folder_paths:
        remove_source_code_from_reports(folder_path)
        remove_reference_to_source_from_html_reports(folder_path)

def remove_source_code_from_reports(report_folder_path: Path) -> None:
    folder_path = report_folder_path / "reports/src"

    if folder_path.exists():
        shutil.rmtree(folder_path)

def remove_reference_to_source_from_html_reports(report_folder_path: Path) -> None:
    for dir_path, _, filenames in os.walk(report_folder_path):
        for filename in filenames:
            if filename.lower().endswith((".html", ".htm")):
                file_path = os.path.join(dir_path, filename)

                with open(file_path, 'r', encoding='utf-8') as f:
                    soup = BeautifulSoup(f, 'html.parser')

                links_to_remove = soup.find_all('a', href=lambda x: x and x.startswith('../src'))

                if not links_to_remove:
                    continue

                for link in links_to_remove:
                    link.unwrap()

                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(str(soup))

@app.command()
def scan(
        input: Path = typer.Option(
            "/input",
            "--input",
            "-i",
            help="Path of a folder that contains individual source code projects as subfolders.",
            exists=True,
            file_okay=False,
            dir_okay=True,
            readable=True,
            writable=True,
        ),
        output: Path = typer.Option(
            "/output/",
            "--out",
            "-o",
            help="Path of a folder where Sokrates reports can be written.",
            file_okay=False,
            dir_okay=True,
            readable=True,
            writable=True
        ),
        report_folder_name: Path = typer.Option(
            "_kitnetic_sokrates_reports",
            "--report-folder-name",
            "-r",
            help="The name of the folder that will be created within the --output folder to store Sokrates reports.",
            file_okay=False,
            dir_okay=True,
            readable=True,
            writable=True
        ),
        dry_run: bool = typer.Option(
            False,
            "--dry-run",
            help="Print commands but do not execute the scanner.",
        ),
) -> None:
    console.print("\n[bold]Sokrates code scanner[/bold]\n")

    if not is_execution_interactive():
        console.print(
            "\n[red]This script is not interactive (No TTY detected).[/red] Please run docker using the '-it' flags."
        )
        raise typer.Exit(code=2)

    try:
        project_folders = list_immediate_subfolders(input)
    except Exception as e:
        console.print(f"\n[red]Error while reading project folders:[/red] {e}")
        raise typer.Exit(code=2)

    if not project_folders:
        console.print("\n[yellow]No project folders found.[/yellow]")
        raise typer.Exit(code=0)

    selected = prompt_for_project_selection(project_folders)
    projects = [prompt_for_project_metadata(p) for p in selected]

    if len(projects) == 0:
        console.print("\n[yellow]No project folders have been selected.[/yellow]")
        raise typer.Exit(code=0)

    scan_results = scan_projects(projects, dry_run=dry_run)

    console.print("\n[bold green]All scans complete.[/bold green]")

    if not dry_run:
        console.print("\n[bold]Finalising...[/bold]\n")

        report_folder_path = output.resolve() / report_folder_name

        console.print(f"[cyan]Stage 1 of 3:[/cyan] Consolidating Sokrates reports")
        consolidate_sokrates_reports(report_folder_path, scan_results)

        console.print(f"[cyan]Stage 2 of 3:[/cyan] Removing source code from Sokrates reports")
        sanitise_html_reports(report_folder_path)

        console.print(f"[cyan]Stage 3 of 3:[/cyan] Creating summary report")
        write_summary_report(report_folder_path, scan_results)

        console.print(f"\n[bold green]All reports written to {report_folder_path}[/bold green]\n")

def main() -> None:
    app()

if __name__ == "__main__":
    main()