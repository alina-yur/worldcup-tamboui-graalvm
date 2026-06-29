# World Cup TamboUI GraalVM Demo

A small terminal app that shows a hardcoded 2026 FIFA World Cup snapshot from
June 29, 2026:

- group standings
- Round of 32 pairings
- emoji flags
- fixed date and integer formatting
- `String.format` in the rendering path
- static tournament data initialized into the native image heap
- reflection metadata via `config/reflect-config.json`

The point is one binary that touches the areas used in the Native Image size
work: metadata, image heap storage, and `String.format`, while staying on the
small-app path that does not use locale formatting.

## Build The Native App

```bash
mvn -Pnative -DskipTests package
```

This writes one executable:

```text
target/worldcup-standings
```

Run it as a native terminal app:

```bash
target/worldcup-standings
```

For plain output:

```bash
target/worldcup-standings --snapshot --group=C
target/worldcup-standings --snapshot --search=Brazil
```

## Compare GraalVM Releases

Build both comparison binaries with SDKMAN:

```bash
scripts/build-graalvm-comparison.sh
```

By default this runs:

```bash
sdk use java 25.0.2-graalce
mvn -q -Pnative -DskipTests -Dnative.image.name=worldcup-standings-25.0-ce package

sdk use java 25.1-graalce
mvn -q -Pnative -DskipTests -Dnative.image.name=worldcup-standings-25.1-ce package
```

It writes:

```text
target/worldcup-standings-25.0-ce
target/worldcup-standings-25.0-ce-build-output.json
target/worldcup-standings-25.1-ce
target/worldcup-standings-25.1-ce-build-output.json
```

You can also use the Maven naming profiles directly:

```bash
sdk use java 25.0.2-graalce
mvn -Pnative,native-25.0-ce -DskipTests package

sdk use java 25.1-graalce
mvn -Pnative,native-25.1-ce -DskipTests package
```

For one-off native-image builds without the Maven native plugin:

```bash
scripts/build-native.sh worldcup-standings-25.1-ce
```

## JVM Fallback

```bash
mvn -q -DskipTests package
mvn -q exec:java -Dexec.args="--snapshot --group=C"
```

Controls in the interactive TUI:

- `n` or right arrow: next group
- `p` or left arrow: previous group
- `/` or `s`: search
- `Tab`: switch focus between standings and fixtures
- up/down or `k`/`j`: select rows
- `Enter`: open the selected fixture's left-side group
- mouse: click group tabs, rows, or the search footer
- `q`: quit
