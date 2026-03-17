# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**KCLI** is a Kotlin library for building type-safe CLI applications with options, subcommands, and typed arguments.
The program exits immediately if there is a problem with the parameters.

## Build and Development Commands

```bash
./gradlew build              # Build the project
./gradlew test               # Run all tests
./gradlew test --tests KcliTest.test_name  # Run specific test
./gradlew clean              # Clean build artifacts
```

## Architecture

### Core Classes

- **Cmd** (`kcli.kt:73-226`): Main entry point that manages options, subcommands, arguments. Parses args via `fromArgs()`, prints help via `printUsage()`
- **CmdOption<T>** (`kcli.kt:308-345`): Generic typed option with converter function, tracks if provided via `given` property
- **CmdArgument** (`kcli.kt:348-380`): Positional arguments with min/max constraints and default values

Both use Kotlin property delegation (`getValue()` operator) for seamless use.

### Typed Option Factories (`kcli.kt:228-306`)

`option()`, `optionInt()`, `optionFloat()`, `optionDouble()`, `optionDate()`, `optionTime()`, `optionBool()`

### Parsing

1. Identifies short options (`-i value`), long options (`--index value`), subcommands, and positional arguments
2. Validates required options, argument counts, and type conversions
3. Executes option callbacks during parsing, then command function

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

## Testing

Tests in `KcliTest.kt` cover parsing, type conversions, callbacks, subcommands, and error handling. Run with `./gradlew test`.

## Notes

- **Java 17+** requirement with official Kotlin code style
- **No external dependencies** beyond Kotlin stdlib and JUnit 5
- **Error handling** uses `error()` for validation with early exit
