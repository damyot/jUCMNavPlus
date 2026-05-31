# jUCMNav

[![Build and publish update site](https://github.com/damyot/jUCMNavPlus/actions/workflows/build-and-deploy.yml/badge.svg?branch=modernization)](https://github.com/damyot/jUCMNavPlus/actions/workflows/build-and-deploy.yml)

Eclipse plug-in for the **User Requirements Notation (URN)** — a graphical
editor and analysis tool combining **Use Case Maps (UCM)** and the
**Goal-oriented Requirements Language (GRL)**. Built on EMF (model), GEF
Classic (diagram editors), and MDT/OCL (constraints).

## Status

Modernization branch for Java 21 / Eclipse 2026-03. The CI badge above
tracks the `modernization` branch; once Phase A merges to `master`, switch
the badge to `?branch=master`.

See [CLAUDE.md](CLAUDE.md) for the orientation document every contributor
(and AI tool) should read first, and [MIGRATION_ERRORS.md](MIGRATION_ERRORS.md)
for the burn-down of issues addressed during modernization.

## Install

Once GitHub Pages is enabled on the repository, install from:

```
https://damyot.github.io/jUCMNavPlus/
```

via Eclipse → Help → Install New Software… → Add → Location: the URL above.

To install from a build that has NOT yet been published to Pages (typical
for PRs from feature branches): download the `jucmnav-update-site` artifact
from the workflow run, unzip it locally, and point Install New Software at
the unzipped folder via the **Local…** button.

## Build

```bash
mvn -B clean verify
```

The installable p2 update site lands at
`seg.jUCMNav.repository/target/repository`. Requires JDK 21 (Adoptium /
Temurin recommended).

On Windows, if `mvn` fails with a PKIX TLS-handshake error fetching from
Maven Central, drop the following two lines into `.mvn/jvm.config`
(gitignored — local override only) to use the Windows certificate store:

```
-Djavax.net.ssl.trustStoreType=Windows-ROOT
-Djavax.net.ssl.trustStore=NUL
```

## Develop in the IDE

In Eclipse → File → Import → Existing Projects into Workspace → root this
repository. Set the target platform via
`seg.jUCMNav.target/seg.jUCMNav.target` (open it, click "Set as Active
Target Platform"). Run As → Eclipse Application launches a child workbench
with the plug-in.

## License

See the in-repo headers and [seg.jUCMNav/about.html](seg.jUCMNav/about.html).
