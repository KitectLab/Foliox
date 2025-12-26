# AGENTS Guide (Foliox Project)

This guide is for repository contributors and AI coding agents (e.g., GitHub Copilot Chat). It clarifies roles, aligns workflows, reduces collaboration overhead, and ensures high quality and consistency across Kotlin Multiplatform (KMP) targets.

## Project Overview

- Tech stack: Kotlin Multiplatform (KMP), Compose Multiplatform, Gradle Kotlin DSL.
- Main modules:
  - [foliox-core/](foliox-core/): Core cross-platform logic and components.
  - [sampleApp/](sampleApp/): Sample applications
	 - [sampleApp/androidApp/](sampleApp/androidApp/): Android sample
	 - [sampleApp/iosApp/](sampleApp/iosApp/): iOS sample
	 - [sampleApp/jvmApp/](sampleApp/jvmApp/): JVM/Desktop sample
	 - [sampleApp/sharedApp/](sampleApp/sharedApp/): KMP shared layer (includes `wasmJsMain`)
- Dependency management: Gradle Version Catalog at [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Roles & Responsibilities

- Maintainers: Own roadmap, releases, final merge decisions, and quality gates.
- Contributors: Implement features and fixes, open PRs, and address reviews.
- AI Agents: Make scoped changes to code and docs, write tests, update dependencies/configs, submit minimal diffs, and provide reproducible run commands and verification steps.

## Workflow (Short)

1. Task intake: Use Issues to define scope, acceptance criteria, and impact; apply labels like `area/*`, `type/*`.
2. Branching: Branch off `main` with one of:
	- `feature/*` for new features
	- `fix/*` for bug fixes
	- `docs/*` for docs changes
	- `chore/*` for tooling/build scripts
3. Commits: Use Conventional Commits
	- `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`, `build:`, `ci:`, `perf:`, `style:`, `revert:`
	- Example: `feat(shared): add paging for repository list`
4. PRs: Follow the “PR Checklist” below; keep scope focused; link the Issue; add “How to verify”.
5. Reviews & merge: At least one maintainer approval, passing CI, and no blocking discussions.

## Local Development & Run

Recommended prerequisites:
- JDK 17+
- Android Studio (latest stable) + Android SDK/NDK as needed
- Xcode 15+ (for iOS run/signing)

Common commands (Windows PowerShell):

```powershell
# List available Gradle tasks
./gradlew.bat tasks

# Full build (all modules)
./gradlew.bat build

# Run the JVM/Desktop sample
./gradlew.bat :sampleApp:jvmApp:run

# Install Android sample (Debug) to a connected device/emulator
./gradlew.bat :sampleApp:androidApp:installDebug

# iOS: open and run in Xcode
Start-Process "sampleApp/iosApp/iosApp.xcodeproj"

# Web/Wasm (if the task exists; confirm via `tasks --all` first)
./gradlew.bat :sampleApp:sharedApp:wasmJsBrowserRun
```

Tip: Task names may vary by KMP/Compose version. If you see “Unknown task”, run `./gradlew.bat tasks --all` to find the equivalent task.

## Code & Directory Conventions

- Cross-platform code: Place in `commonMain` (e.g., [foliox-core/src/commonMain/](foliox-core/src/commonMain/)).
- Platform-specific code: Place in `androidMain`, `iosMain`, `jvmMain`, `wasmJsMain`, etc.
- UI: Compose Multiplatform. Prefer implementing UI and state in the shared layer; keep platform layers thin as containers/launchers.
- Resources & localization: The project includes KMP and Compose resource generation (e.g., [composeApp/build/generated/compose/resourceGenerator/](composeApp/build/generated/compose/resourceGenerator/)). When adding resources, update the corresponding module resource directories and references.
- Language policy: Except for i18n resource files, all comments, commit messages, documents, changelog, and Markdown files must be in English. Other languages should not appear unless explicitly specified.

## Dependency Management (Version Catalog)

Add/upgrade dependencies only in [gradle/libs.versions.toml](gradle/libs.versions.toml), then reference via `libs.xxx` in module `build.gradle.kts` files. Example:

```toml
[versions]
compose = "2025.1.0"

[libraries]
compose-runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "compose" }
```

```kotlin
dependencies {
	 implementation(libs.compose.runtime)
}
```

Avoid hardcoding coordinates/versions in individual modules (“snowflake versions”).

## Testing & Quality

- Run all unit tests:

```powershell
./gradlew.bat test
```

- Per-module execution examples: `:foliox-core:test`. Add platform-specific tests as needed (Android instrumented tests, iOS targets, etc.).
- Code style: Follow Kotlin style and Compose best practices. If adding static analysis/formatting (ktlint, detekt), mention it in the PR and integrate with CI.

## PR Checklist (Author)

- Clear scope: Single responsibility; avoid broad changes in one PR.
- Build passes: Validate locally via `build` and relevant `run` tasks.
- Tests: Include unit tests for new/changed logic (or justify exceptions).
- Docs: Update [README.md](README.md), this file, or module docs where needed.
- Minimal changes: Avoid unrelated reorders/renames; preserve existing style.
- Commits: Use Conventional Commits; concise, searchable PR title.
- Demo steps: Provide “How to verify” commands or GIFs/screenshots.

## Review Guidelines (Reviewer)

- Correctness: Logic boundaries, concurrency/lifecycle soundness (esp. Compose & multiplatform).
- Maintainability: Clear module boundaries; stable public APIs; consistent naming/style.
- Impact: Compatibility risks; need for release notes or migration guides.
- Performance/size: Obvious multi-target performance or binary size risks.
- Security/privacy: New external services/permissions; minimal data collection.

## Definition of Done

- Acceptance criteria met and reproducible (complete commands and steps).
- Key paths covered by tests or manual verification scripts.
- Docs and samples updated accordingly.
- CI green with no high-priority open discussions.

## AI Agent Playbook

Follow these steps to avoid overreach and irreproducible outcomes:

1. Clarify the task: Confirm goals, constraints, output format, verification steps, and risks.
2. Impact assessment: List affected modules and source sets (commonMain vs platform-specific).
3. Minimal changes: Modify only strongly related code/config/docs.
4. Dependencies/scripts: Use the Version Catalog; do not hardcode per-module versions.
5. Synchronize docs: If adding APIs/config, update README, samples, and this guide.
6. Verify and record: Provide copy-paste commands, expected outputs/behaviors, and logs/screenshots.
7. Deliverables:
	- Focused code changes
	- Related tests/samples
	- Run and verification commands
	- Migration notes (if breaking changes)

## Task Playbooks

### Add a third-party dependency

1) Add version and coordinates in [gradle/libs.versions.toml](gradle/libs.versions.toml).
2) Reference it via `libs.xxx` in the target module `build.gradle.kts`.
3) Validate locally with `./gradlew.bat build`.

### Add cross-platform resources/localization

1) Place resources in the shared layer or platform resource directories.
2) If resources are generated/synced, run corresponding tasks or update docs.
3) Access resources via shared abstractions in UI; avoid platform-only calls from shared code.

### Add shared UI (Compose)

1) Add `@Composable` components and state models in the shared layer.
2) Mount the component in Android/iOS/JVM launchers (keep platform layers thin).
3) Use `./gradlew.bat :sampleApp:jvmApp:run` to quickly validate views/interactions.

### Fix a platform-specific bug

1) Diagnose and fix in the platform source set (e.g., `androidMain`) first.
2) If shared logic is involved, abstract it into `commonMain` with platform adapters.
3) Include a minimal reproduction and verification commands to prevent regressions.

## Versioning & Releases (Suggested)

- Use semantic versioning: MAJOR.MINOR.PATCH.
- Call out important changes in Release Notes with migration guides and examples; update samples if affected.

## Incident Handling & Rollback

- For potentially breaking changes, add a feature flag or maintain a backward-compatible path.
- If issues arise post-release: prefer reverting the PR, or create a `fix/*` hotfix branch.

---

If you want to add CI, PR templates, or code style tools (ktlint/detekt/spotless), please open an Issue and link to the intended config locations to keep the repository consistent.
