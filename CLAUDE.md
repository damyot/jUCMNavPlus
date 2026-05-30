# CLAUDE.md — jUCMNav modernization

This file orients every Claude Code session working in this repo. Read it before
making changes.

## What this project is

jUCMNav is an Eclipse plug-in (graphical editor + analysis tool) for the User
Requirements Notation (URN), combining Use Case Maps (UCM) and the Goal-oriented
Requirements Language (GRL). It is built on **EMF** (model) and **GEF Classic**
(diagram editors), and uses **MDT/OCL** for constraints. It is a classic PDE
plug-in: `MANIFEST.MF`, `plugin.xml`, an EMF model under `model/`
(`.ecore` + `.genmodel`), and editor code under `src/`.

The project last built reliably in 2018, had a partial fix attempt in 2020, and
is currently broken: compile errors against modern Java/Eclipse, behind on
versions, and no working deployment (the old update site is gone).

## Targets (do not change without asking)

- **Java 21 LTS** — source/target level 21. Do NOT raise the language level past 21.
- **Eclipse 2026-03 (4.39)** platform, via the p2 release repo.
- **GEF Classic 3.x** (the maintained continuation) — NOT GEF4/Zest. Keep the
  existing EditPart/Figure/draw2d code; migrate, don't rewrite.
- **Tycho 5.0.3** for the command-line build and the p2 update site.

## Golden rules

1. **The build is the source of truth.** Don't reason about Eclipse/EMF/GEF APIs
   from memory — they have drifted over 20 years. Compile, read the actual
   errors, fix, recompile.
2. **Never hand-edit generated EMF code.** If model code is wrong, fix the
   `.ecore`/`.genmodel` and regenerate. Generated files are recognizable by the
   `@generated` tag. Hand-written extensions use `@generated NOT` — preserve those.
3. **Small, compilable, single-purpose commits.** One error cluster per commit so
   regressions can be bisected. Don't batch unrelated fixes.
4. **Preserve behavior.** This is a migration, not a redesign. If a fix changes
   what the tool does (e.g. a GRL evaluation result), stop and flag it for human
   review rather than guessing.
5. **Prefer the smallest change that compiles and runs.** Don't refactor for style
   while migrating.

## Build & run

```bash
# Full headless build + tests + p2 update site:
mvn -B clean verify

# The installable update site (p2 repository) lands here:
#   seg.jUCMNav.repository/target/repository
```

In the IDE: import as existing projects, set the target platform to
`seg.jUCMNav.target/seg.jUCMNav.target`, then Run As → Eclipse Application to
launch a runtime workbench with the plug-in. Run As → JUnit Plug-in Test for tests.

## Repo layout (after scaffolding)

- `seg.jUCMNav/` — the plug-in (existing code). Pomless: built from its MANIFEST.MF.
- `seg.jUCMNav.feature/` — the installable feature (what users select).
- `seg.jUCMNav.repository/` — produces the p2 update site (`category.xml`).
- `seg.jUCMNav.target/` — target platform definition (pinned to 2026-03).
- `pom.xml` — Tycho parent/aggregator.
- `.mvn/extensions.xml` — enables Tycho pomless builds.
- `.github/workflows/` — CI build + publish update site to GitHub Pages.

## Known migration hotspots (verify each against real errors)

- **`Bundle-RequiredExecutionEnvironment`** in `MANIFEST.MF`: change `J2SE-1.5`
  (or similar) to `JavaSE-21`.
- **Removed JDK APIs** — the most likely compile-breakers:
  - JAXB (`javax.xml.bind.*`) was removed from the JDK after Java 8. If Z.151
    XML import/export uses it, add Jakarta XML Binding as an explicit dependency.
  - `com.sun.*` / `sun.misc.*` internals — replace with supported APIs.
- **`Require-Bundle` / `Import-Package` version ranges** that no longer resolve
  against the 2026-03 platform.
- **MDT/OCL**: the `org.eclipse.ocl` API shifted significantly across versions —
  expect this to need real work, not mechanical fixes. Flag it early.
- **Eclipse 3.x → 4.x platform**: deprecated action-set/`IActionDelegate`
  patterns, Forms API, assorted workbench calls. Fix compile-blockers first;
  defer deprecation-only warnings.
- **Third-party export libs** (PDF/SVG/reporting jars, e.g. iText/Batik): may
  need updated OSGi-friendly versions.

## Workflow expectation

First task in a fresh session: produce/refresh `MIGRATION_ERRORS.md` — a
burn-down list of all current compile errors grouped by root cause. Work the
list cluster by cluster, committing after each clears. Don't declare Phase A done
until `mvn clean verify` reaches zero compile errors.

## Do not

- Do not bump the Java language level above 21.
- Do not switch GEF Classic to GEF4/Zest.
- Do not edit `@generated` EMF files by hand.
- Do not introduce network calls, telemetry, or new runtime dependencies without
  asking.
- Do not silence errors by deleting features/tests — fix or explicitly flag them.
