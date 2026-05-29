# Plan: Math Blaster — a Kotlin/Wasm retro arcade math shooter

## Context

This is a greenfield project. The goal is an educational, retro arcade **shooter** that runs
in the browser as real **WebAssembly**. The user knows Kotlin and TypeScript and chose Kotlin.

**Game idea:** You are given a target number. Signed numbers (-10..10) drift down toward your
ship. You shoot them to add their value to a running **accumulator**. Land on the target
*exactly* to clear the round. **Overshooting busts you and costs a life.** Negatives let you
correct (target 8 = `+4 +4` or `+9 -1`). Difficulty ramps: Level 1 is addition/subtraction
(signed numbers), Level 2 introduces multiplication, later levels add speed and range. Simple
arithmetic, gamified.

## Toolstack decision

- **Language:** Kotlin. **Engine:** KorGE 6.x (Kotlin Multiplatform 2D game engine).
- **Target:** `wasmJs` (WasmGC) — genuine WebAssembly; supported in all major browsers.
- **Build tool:** Gradle (Kotlin DSL). KorGE + KMP + Wasm is Gradle-only, so this project
  deviates from the global Maven preference. The deviation is recorded in `CLAUDE.md`.
- **Dev loop:** iterate on the JVM target (`runJvm` / `runJvmAutoreload`); ship and verify on
  the Wasm web target.
- KorGE APIs relied on (confirmed in current docs): `Korge { }` + `sceneContainer()`, `Scene`
  with `sceneMain()`, `keys { down(Key.X){} }` / `onKeyDown` (use `justDown`), `addUpdater { }`,
  `onCollision`, `Text`/`solidRect`/`image`/`sprite` views, and the `wasmJsBrowser*` tasks.

## Step 0 — CLAUDE.md (done)

The project `CLAUDE.md` capturing the game idea, toolstack, the Gradle exception, conventions,
and commands has been written.

## Step 1 — Scaffold the KorGE project

Use the KorGE Forge "New Project" wizard with the `wasmJs` target enabled, OR start from an
official KorGE starter / the `org.korge` Gradle plugin (v6.x; note the namespace migrated from
`com.soywiz.korge` to `org.korge`). After scaffolding, **verify the exact Gradle task names and
plugin version in the generated `build.gradle.kts`** rather than assuming them. Confirm the JVM
run task, the Wasm web tasks (`wasmJsBrowserDevelopmentRun`, `wasmJsBrowserDistribution`), and
that game code lives in `src/commonMain/kotlin`.

## Step 2 — Architecture (small, single-purpose files)

```
src/commonMain/kotlin/
  main.kt                       # Korge { } entrypoint -> SceneContainer -> MenuScene
  game/
    model/
      GameState.kt              # lives, score, level, accumulator, currentTarget
      OperationToken.kt         # data class: operator (+,-,x) + operand; apply(acc): Int
      LevelConfig.kt            # PARAM OBJECT: operators, operandRange, enemySpeed,
                                #   spawnIntervalMs, targetsToAdvance
    logic/                      # PURE Kotlin, NO KorGE types -> unit-testable
      LevelConfigs.kt           # configFor(level): LevelConfig (L1 +/-, L2 adds x, ...)
      TargetGenerator.kt        # pick a target reachable with the level's tokens
      RoundEvaluator.kt         # applyToken -> CLEARED | BUST | CONTINUE
    view/
      PlayerShip.kt             # bottom ship, LEFT/RIGHT move, SPACE fire (rate-limited)
      Bullet.kt                 # travels up
      EnemyView.kt              # drifting token; renders its OperationToken label
      Hud.kt                    # Text views: Target, Acc, Lives, Score, Level
    scene/
      MenuScene.kt              # title + "press SPACE to start"
      GameScene.kt              # wires input + spawner + addUpdater loop + collisions
      GameOverScene.kt          # final score + restart
src/commonTest/kotlin/
  RoundEvaluatorTest.kt         # exact -> CLEARED, overshoot -> BUST, else CONTINUE
  TargetGeneratorTest.kt        # generated targets always solvable within range
  LevelConfigsTest.kt           # L1 has no x; L2 includes x; speed/range ramp upward
```

Rationale: the `logic` package holds the math/rules with no engine dependency, so the
arithmetic that makes this educational is covered by fast unit tests. The `view`/`scene`
packages hold the KorGE glue.

## Step 3 — Gameplay rules

- **Core loop:** shoot a token -> `RoundEvaluator.applyToken`:
  - `CLEARED` when accumulator == target: score += 1, new target, advance level after
    `targetsToAdvance` clears.
  - `BUST` when the shot moves the accumulator past the target (overshoot): lives -= 1, reset
    accumulator to 0. At 0 lives -> `GameOverScene`.
  - `CONTINUE` otherwise.
- **Tokens:** `OperationToken(operator, operand)`. L1 spawns only `+`/`-` with operand 1..10
  (signed values -10..10). L2 mixes in `x` tokens (x2, x3). Each enemy renders its token text.
- **Input:** LEFT/RIGHT move the ship; SPACE fires (rate-limited so holding does not autofire).
- **Enemies reaching the bottom:** despawn, no penalty (keeps focus on the arithmetic);
  trivial to change to a life penalty later.
- **Level progression:** `LevelConfigs.configFor(level)` raises speed, shrinks spawn interval,
  widens operand range, and unlocks operators (L1 +/-, L2 +x, L3+ faster/bigger).

## Step 4 — Verification

1. **Unit tests:** `./gradlew jvmTest`. Assert exact->CLEARED, overshoot->BUST,
   partial->CONTINUE; every generated target solvable; L1 excludes x, L2 includes it, ramps up.
2. **JVM run:** `./gradlew runJvm`. Verify movement, firing, accumulation, exact-clear,
   bust/life-loss, level-up, game over/restart.
3. **Wasm web build (the deliverable):** `./gradlew wasmJsBrowserDistribution` (and/or
   `wasmJsBrowserDevelopmentRun`). Serve the output and open in a browser; confirm it loads as
   WebAssembly, keyboard input works, and a full round plays (accumulate to exact clear, plus
   an overshoot bust).

## Open defaults (tunable)

- Starting lives: 3. Clears per level: 5. Constants in `LevelConfig`.
- No round timer initially (overshoot bust is the pressure); a per-level countdown can be added
  later via `LevelConfig` if more tension is wanted.
