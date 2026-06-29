# Repository Notes For Codex

## Goal

This repo is a visual TamboUI + GraalVM Native Image demo for the GraalVM
25.1 native image size work.

The app should remain a good-looking terminal UI for a hardcoded 2026 FIFA
World Cup snapshot:

- group standings
- Round of 32 fixtures
- emoji flags
- colors
- search
- keyboard navigation
- mouse click regions where supported

At the same time, it should be shaped so size comparisons between GraalVM 25.0
and 25.1 have a chance to show relevant improvements.

## What We Tried

The first version used TamboUI/JLine and a localized rendering path:

- `Locale.getDefault()`
- `NumberFormat`
- localized `DateTimeFormatter`
- `String.format(locale, ...)`
- extra locale note data

That was the wrong shape for the “small apps that do not use locales are
smaller” message. It made the app a mixed stress case: TamboUI/JLine, emoji,
reflection, image heap data, and locale formatting.

We removed the locale-heavy path:

- no `Locale.getDefault()`
- no `NumberFormat`
- no localized `DateTimeFormatter`
- no locale-note data
- no visible `locale en` style output
- fixed date strings such as `Jun 29, 2026`
- integer formatting with `Integer.toString(...)`

We also explored making the image-heap/metadata delta more pronounced by adding
more app-domain metadata. That direction was stopped because it risks muddying
the result. The metadata PRs reduce Native Image internal metadata storage; a
large artificial app metadata catalog is not necessarily a fair proof point.

## What Worked

The TamboUI version works well as a visual demo. It is pleasant to run and has
real interaction:

- `/` or `s` search
- `Tab` switches focus
- up/down or `k`/`j` selects rows
- `Enter` on a fixture jumps to the left-side group
- group tabs and rows are clickable in terminals that support mouse events
- `--snapshot` gives scriptable output
- `--search=<text>` starts with a filter

The JVM build and snapshot mode have been smoke-tested.

## What Did Not Work

The TamboUI/JLine binary is not a clean reproduction of the large HelloWorld
size drop.

Measured exact file sizes from one comparison:

```text
target/worldcup-standings-25.0: 17,456,912 bytes
target/worldcup-standings-25.1: 17,325,440 bytes
delta: 131,472 bytes
delta: 0.75%
```

`ls -lh` rounds both to roughly `17M`, so it can look like there is no change.
Use exact byte sizes.

Section inspection showed:

```text
__TEXT / code decreased by about 1 MiB
__DATA / image heap increased by about 0.9 MiB
net reduction was about 0.125 MiB
```

So this app did benefit slightly, but the TamboUI/JLine/resource/image-heap
surface dominates the result.

## Current Direction

Keep the TamboUI app, but bias it toward GR-76005:

> Improve Native Image support for constant `String.format` and
> `String::formatted` calls by intrinsifying simple format strings in CE.
> This reduces reachability of JDK formatting and localization code for apps
> such as HelloWorld.

The current code routes visible formatting through `WorldCupFormatText`, which
uses constant, simple `String.format(...)` and `String::formatted(...)` call
sites. This is intentional. Do not casually replace those calls with manual
string concatenation if the goal is to measure the GR-76005 effect.

Keep the app free of explicit locale APIs unless the user asks for a locale
stress case.

## Build And Compare

Standard native build:

```bash
mvn -Pnative -DskipTests package
```

Comparison script:

```bash
scripts/build-graalvm-comparison.sh
```

That script expects SDKMAN candidates:

```text
25.0.2-graalce
25.1-graalce
```

It writes comparison binaries under `target/`.

When comparing sizes, use exact bytes:

```bash
stat -f '%z %N' target/worldcup-standings-25.0-ce target/worldcup-standings-25.1-ce
```

or the script output. Do not rely on `ls -lh` for the final claim.

## Changelog Context

Relevant GraalVM size-work items discussed while shaping this demo:

- GR-75925: compact Native Image module metadata collections and runtime
  dynamic-access metadata in the image heap.
- GR-75511: reduce image heap symbol and map storage.
- GR-76005: intrinsify simple constant `String.format` and
  `String::formatted` calls in CE to reduce formatter/localization reachability.

For this repo, GR-76005 is the most promising lever because the app can keep its
visual TamboUI behavior while increasing constant simple formatting call sites.

## Guardrails

- Preserve the TamboUI version unless explicitly asked to replace it.
- Do not add a web UI.
- Do not add locale formatting by default.
- Do not add huge random data blobs just to inflate image size.
- Avoid artificial metadata catalogs unless the user explicitly wants a metadata
  stress case.
- Keep snapshot mode working; it is the easiest smoke test.
- If asked to prove the 5 MB HelloWorld-style drop, create a separate tiny CLI
  or HelloWorld-style app. Do not expect this TamboUI/JLine app to show that
  magnitude cleanly.
