# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**KCLI** is a Kotlin library for building type-safe CLI applications with options, subcommands, and typed arguments.
The program exits immediately if there is a problem with the parameters.
Don't use any outer libraries as project dependencies. Only use the Kotlin standard library and JDK APIs.

## Build and Development Commands

There is **no `gradlew` wrapper**. Use the system `gradle` command from the project directory:

```bash
gradle build              # Build the project
gradle test               # Run all tests
gradle clean              # Clean build artifacts
```

Or from any directory using the `-p` flag:
```bash
gradle -p /Users/jlovas/workspace.cpi/kotlin.git/kcli test
```

### Project build details
- Gradle build file: `build.gradle.kts`
- Kotlin version: 2.1.10, JVM target: Java 17
- Kotlin code style: `official` (gradle.properties)
- Package: `hu.jlovas.kcli`
- Test framework: JUnit 5 (Jupiter) via `useJUnitPlatform()`
- Test source code folder: `src/test/kotlin/`

### Source layout
- Active sources: `src/main/kotlin/kcli/kcli.kt`
- Active tests:   `src/test/kotlin/KcliTest.kt`
- **`bin/` directory is NOT an active source set** — ignore it

## Architecture

### Parsing (`Cmd.fromArgs`)

1. Parses short options (`-i value`) and long options (`--index value`) first
2. If subcommands are registered, next token must match a subcommand name — delegates remaining args to it
3. Otherwise remaining tokens are treated as positional arguments
4. Validates required options, argument counts, and type conversions
5. Executes option callbacks during parsing, then the command function

## Key Patterns

**Property Delegation**: Options/arguments use Kotlin's `by` operator:
```kotlin
val mainCmd = Cmd("app", "Description")
val opFile by mainCmd.option("f", "file", "File path", "./default.txt")
```

**Fluent Builder**: Subcommands chain configuration:
```kotlin
val cmd = mainCmd.subCmd("action", "Description").apply {
    val opFlag by optionBool("v", "verbose", "Verbose output")
    function { /* logic */ }
}
```

**Type Safety**: Generic `CmdOption<T>` with converters ensures strong typing with validation.
