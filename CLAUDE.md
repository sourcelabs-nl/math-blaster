# CLAUDE.md

Project instructions for this repository. These build on the global instructions in
`~/.claude/CLAUDE.md`. Where they differ, the exception is called out explicitly below.

## What this project is

**Math Blaster** is an educational, retro **arcade shooter** that runs in the browser as
real **WebAssembly**.

This project is built for a game-jam competition. The full rules and judging criteria live in
`@docs/competition-rules.md` (retro styling, must be educational, browser-playable, local
storage only, no backend, hosted on an office-accessible webserver).

### Game concept

- You are given a **target** number. Signed numbers (-10..10) drift down toward your ship.
- You **shoot** numbers to add their value to a running **accumulator**.
- Land on the target **exactly** to clear the round and get a new target.
- **Overshooting busts you and costs a life** (start with 3 lives; accumulator resets to 0).
- Negatives let you correct, so target `8` can be solved as `+4 +4` or `+9 -1`.
- Difficulty ramps by level: **Level 1** is addition/subtraction (signed numbers), **Level 2**
  introduces **multiplication**, later levels increase speed, spawn rate, and number range.
- Simple arithmetic, gamified.

## Toolstack

- **Language:** Kotlin.
- **Engine:** KorGE 6.x (Kotlin Multiplatform 2D game engine).
- **WebAssembly target:** `wasmJs` (WasmGC). Produces genuine WASM; all major browsers
  support WasmGC since late 2024.
- **Dev/iteration target:** JVM (faster build/run loop than the web target).
- Game code lives in `src/commonMain/kotlin` so it compiles to every target, including Wasm.

### Build tool exception (overrides the global Maven preference)

The global instructions prefer **Maven**. **This project uses Gradle (Kotlin DSL).** KorGE +
Kotlin Multiplatform + Wasm is Gradle-only in practice and does not work with Maven, so Gradle
is required here. This is the one accepted deviation; the rest of the global rules still apply.

## Conventions

- Prioritize simplicity and readability over clever solutions; code is read more than written.
- Small, single-purpose files; organize code into separate files where appropriate.
- **Keep pure game logic free of KorGE types** (in a `logic`/`model` package) so the
  arithmetic and level rules are covered by fast unit tests in `commonTest`.
- Use **parameter objects** when a signature grows wide; e.g. per-level rules live in a
  `LevelConfig` data class rather than long positional argument lists.
- No em-dashes in prose or documentation. Use commas, colons, or parentheses.
- Do not do large refactors unless explicitly asked.

## Project layout (target)

```
src/commonMain/kotlin/
  main.kt                 # Korge { } entrypoint -> SceneContainer -> MenuScene
  game/
    model/                # GameState, OperationToken, LevelConfig (param object)
    logic/                # PURE: LevelConfigs, TargetGenerator, RoundEvaluator (no KorGE)
    view/                 # PlayerShip, Bullet, EnemyView, Hud (KorGE views)
    scene/                # MenuScene, GameScene, GameOverScene
src/commonTest/kotlin/    # RoundEvaluatorTest, TargetGeneratorTest, LevelConfigsTest
```

## Commands

Exact task names should be confirmed against the generated `build.gradle.kts`, since KorGE
coordinates and tasks can drift between versions.

- Unit tests (logic): `./gradlew jvmTest`
- Run on JVM (fast manual loop): `./gradlew runJvm` (or `runJvmAutoreload`)
- Build the Wasm web distribution: `./gradlew wasmJsBrowserDistribution`
- Live dev server (Wasm): `./gradlew wasmJsBrowserDevelopmentRun`

## KorGE API notes (confirmed from current docs)

- Entrypoint: `suspend fun main() = Korge { ... }` with `sceneContainer()`.
- Scenes: subclass `Scene`, implement `sceneMain()`; navigate with `changeTo`/`pushTo`/`back`.
- Input: `keys { down(Key.LEFT) { } }` / `onKeyDown`; use `justDown` to avoid auto-repeat.
- Game loop: `addUpdater { }` runs every frame; update positions and run logic there.
- Collisions: `view.onCollision({ predicate }) { }`.
- Views: `text(...)`, `solidRect(...)`, `image(...)`, `sprite(...)`.