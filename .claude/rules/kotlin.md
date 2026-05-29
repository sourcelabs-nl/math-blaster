---
paths:
  - "src/**/*.kt"
---

# Kotlin source code rules

Rules for working with Kotlin source files in this repository. These apply to all
`*.kt` files under `src/`.

## Functional package organization

- Organize code into **functional packages** grouped by responsibility/feature, not by type.
  In this project that means `model` (data), `logic` (pure rules), `view` (KorGE views),
  `scene` (KorGE scenes). A new concern gets its own package.
- **Keep pure game logic free of KorGE types.** Arithmetic, level rules, and state transitions
  live in `logic`/`model` so they are covered by fast unit tests in `commonTest`. Only `view`
  and `scene` may import `korlibs.*`.
- One public type per file, named after the type. Small, single-purpose files.

## Single responsibility

- A function should **do one thing** and have a single reason to change. The function name
  should fully describe what it does; if you need "and" to describe it, split it.
- Functions may **compose** other functions: a higher-level function reads as a short list of
  calls to single-purpose helpers. The single-responsibility principle still applies to the
  composing function (its one job is the orchestration).
- Side effects and decisions belong in separate functions from pure computation where it aids
  clarity.

## Function length

- A function should be **at most ~30 lines** of body. If it grows past that, it is usually
  doing more than one thing: extract well-named private helpers until each function reads
  top-to-bottom as a short list of steps.
- Prefer many small, named functions over one long function with comment-separated sections.
  The function name is the comment.

## Type and file size

- Keep one clear responsibility per class. As a rule of thumb, a `.kt` file over ~200 lines, or
  a class with more than ~15 methods, is a signal it has absorbed several responsibilities: find
  a cohesive cluster of fields and methods and extract it into its own type.
- **Scenes grow fastest.** Keep a `Scene` as a thin orchestrator (lifecycle, wiring, phase and
  screen transitions) and push the simulation (entities, movement, collisions) into its own
  engine-facing class, and any pure text/state editing into `logic`. For example, the live
  playfield lives in `Playfield`, not in `GameScene`.
- Split along responsibility seams (input, simulation, presentation, flow), not at an arbitrary
  line count: the line and method counts are smells that prompt the look, not hard limits.

## Signatures and parameters

- When a parameter list grows wide (more than 4-5 related parameters), wrap them in a
  parameter object / data class (e.g. `LevelConfig`) instead of adding positional arguments.

## Style

- Match the surrounding code: indentation, naming, and idiom.
- Prioritize simplicity and readability over clever solutions; code is read more than written.
- No em-dashes in comments or KDoc. Use commas, colons, or parentheses.

## Kotlin idioms

Prefer the idiomatic constructs from the official idioms guide
(https://kotlinlang.org/docs/idioms.html). Apply these where they fit:

- Use `data class` for DTOs/value holders to get `equals`/`hashCode`/`toString`/`copy`/destructuring for free.
- Provide default values for function parameters instead of overloads.
- Prefer functional collection operations (`filter`, `map`, etc.) and `in`/`!in` for membership checks.
- Use string interpolation (`"Name $name"`) over concatenation.
- Read input safely with `readln().toIntOrNull()` and similar `...OrNull` conversions.
- Use `when` with `is` for type-based branching; prefer `when`/`if`/`try` as expressions that return a value.
- Default to read-only collections (`listOf`, `mapOf`); destructure when traversing (`for ((k, v) in map)`).
- Use ranges (`1..100`, `1..<100`, `downTo`, `step`) for iteration.
- Use `by lazy { }` for lazily computed properties.
- Add behavior to existing types with extension functions rather than utility classes.
- Use `object` for singletons and `@JvmInline value class` for type-safe primitive wrappers.
- Handle nullability with `?.`, the Elvis operator (`?:`), `?.let { }`, and `firstOrNull()` rather than manual null checks.
- Use `?: throw ...` (or `?: return`) to fail fast on null.
- Use scope functions appropriately: `apply`/`also` for configuration, `with` for grouping calls, `let` for transforms.
- Write single-expression functions (`fun answer() = 42`) when the body is one expression.
- Use `.use { }` for closeable resources (try-with-resources equivalent).
- Use `reified` type parameters in `inline` functions when runtime type info is needed.
- Mark unfinished code with `TODO()` rather than placeholder stubs that silently pass.