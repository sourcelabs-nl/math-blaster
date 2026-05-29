---
name: korge
description: >-
  Build 2D games and graphical apps with KorGE, the Kotlin multiplatform game engine
  (targets JVM desktop, Android, iOS, JS, and WASM web). Use this whenever you are
  writing or modifying KorGE code — setting up a KorGE project, writing a `suspend fun main() = Korge { ... }`
  entry point, creating Scenes, adding Views (image, text, solidRect, sprite, container),
  handling mouse/keyboard/touch input, doing tweens/animations, loading resources with
  resourcesVfs/KR, building UI with uiButton/uiText, tile maps, sprite animations, or
  saving game data. Trigger on imports like `korlibs.korge.*`, `korlibs.image.*`,
  `korlibs.math.*`, on files that call `Korge(...)` or extend `Scene()`, and any time the
  user mentions KorGE, korlibs, soywiz, or asks to make a game in Kotlin.
---

# KorGE — Kotlin Multiplatform Game Engine

KorGE (by korlibs) is an open-source 2D game engine written in Kotlin. It targets desktop
(JVM + Kotlin/Native: Windows, macOS, Linux), web (JS + WASM), and mobile (Android, iOS)
from a single common codebase. Its display model is a **tree of `View` nodes** rendered with
the painter's algorithm, very much like Flash's DisplayObject or the HTML DOM.

The API is **DSL-heavy and coroutine-based**: the entry point is a `suspend fun main`, views
are created with builder functions that take the parent `Container` as receiver, and timing
(tweens, delays, sound) integrates with Kotlin coroutines.

## When to reach for the reference files

Keep this file in context for the mental model and common patterns. Read the matching
reference file in `references/` when you go deep on a topic — they contain the full APIs:

| Topic | File |
|-------|------|
| Project setup, Gradle build files, targets, running, deploying | `references/project-setup.md` |
| Entry point, Scenes, lifecycle, transitions, navigation, injector | `references/scenes.md` |
| The View tree, properties, every standard view, alignment, zIndex | `references/views.md` |
| Mouse / keyboard / touch / gamepad input, drag & drop, dragging | `references/input.md` |
| Tweens, animator, easings, updaters | `references/animation.md` |
| Resources (KR, resourcesVfs), bitmaps, sounds/music, fonts, sprites, tile maps | `references/resources.md` |
| UI components and layouts (buttons, stacks, grids, scroll, styling) | `references/ui.md` |
| Resolution/virtual size, persistent storage/preferences, testing | `references/advanced.md` |

## Core mental model

1. **Entry point** — `suspend fun main() = Korge { ... }`. The lambda receiver is the `Stage`
   (the root `Container`). For anything beyond a toy, immediately hand off to a `SceneContainer`.
2. **Views** — everything visible is a `View`. `Container` is a `View` that holds children.
   Views are created with DSL builders (`solidRect`, `image`, `text`, `container`, ...) that
   add themselves to the receiver container and return the created view.
3. **Scenes** — a `Scene` is a controller for one screen/level. You override `sceneMain()`
   (and optionally `sceneInit()`) with `SContainer` as receiver and build your views there.
4. **Coroutines** — `sceneMain`, `onClick`, tweens, sound playback, and resource loading are
   all suspend-friendly. Loading happens with `resourcesVfs["..."].readBitmap()` etc.
5. **Virtual resolution** — you design against a fixed `virtualWidth`/`virtualHeight`; KorGE
   scales it to the real window. You rarely deal with raw pixels.

## Minimal hello world

```kotlin
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*

suspend fun main() = Korge(virtualWidth = 640, virtualHeight = 480, backgroundColor = Colors["#2b2b2b"]) {
    solidRect(100, 100, Colors.RED).xy(50, 50)
    text("Hello KorGE").centerOnStage()
}
```

## Idiomatic structure (use this for anything real)

Split the game into scenes and route between them through a `SceneContainer`. Build views in
`sceneMain`; do blocking resource loads in `sceneInit`.

```kotlin
suspend fun main() = Korge(virtualWidth = 640, virtualHeight = 480) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { MainMenuScene() }
}

class MainMenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        text("My Game", textSize = 48.0).centerXOnStage().y(80.0)
        uiButton("Play") {
            centerOnStage()
            onClick { sceneContainer.changeTo { GameScene() } }
        }
    }
}

class GameScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val player = solidRect(40, 40, Colors.LIME).xy(300, 220)
        addUpdater { dt ->
            val speed = 200.0 * (dt.seconds)        // dt is a TimeSpan; scale by delta time
            if (input.keys[Key.LEFT])  player.x -= speed
            if (input.keys[Key.RIGHT]) player.x += speed
        }
    }
}
```

## Patterns worth internalizing

- **DSL builders add-and-return.** `val r = solidRect(w, h, color)` both adds `r` to the
  current container and gives you the reference. The trailing lambda configures the view with
  it as receiver: `solidRect(w, h, color) { xy(10, 10); alpha = 0.5 }`.
- **Position/transform helpers** are chainable extensions: `.xy(x, y)`, `.position(x, y)`,
  `.scale(2.0)`, `.rotation(45.degrees)`, `.anchor(Anchor.CENTER)`, `.centerOnStage()`,
  `.alignLeftToRightOf(other)`. Discover more via IDE autocomplete (`align___To___Of`).
- **Per-frame logic** goes in `view.addUpdater { dt -> }` (variable delta) or
  `addFixedUpdater(60.timesPerSecond) { }`. Always scale movement by the delta time so the
  game runs the same speed regardless of frame rate. See `references/animation.md`.
- **Input** has two styles: poll state (`input.keys[Key.SPACE]`, `input.mouse`) inside an
  updater, or attach events (`view.onClick { }`, `view.keys { down(Key.X) { } }`). See
  `references/input.md`.
- **Separate logic from presentation.** Model the game as plain data + pure state transitions,
  then render those with views/tweens. This keeps the core unit-testable without a window.
  See the testing section in `references/advanced.md`.
- **Units.** Angles use `.degrees` / `.radians`; durations use `.seconds` / `.milliseconds`
  / `60.timesPerSecond` (these come from korlibs `Angle` and `TimeSpan`). Colors come from
  `Colors.RED`, `Colors["#ff8800"]`, or `MaterialColors.BLUE_500`.

## Conventions for writing KorGE code in this project

- Build with **Maven only if the project already does**; KorGE itself is distributed and
  configured through its **Gradle plugin** (`com.soywiz.korge`) — the official tooling is
  Gradle-based, so a fresh KorGE project uses Gradle. Don't try to convert KorGE's
  multiplatform build to Maven. (This is the one place the global "prefer Maven" rule does not
  apply — KorGE's plugin and multiplatform targets only ship for Gradle.)
- Pin the KorGE version in `gradle/libs.versions.toml` and read the actual version there
  before assuming an API — KorGE moves fast and package names changed across major versions
  (older `com.soywiz.korlibs.*` → current `korlibs.*`). When unsure of an exact symbol, prefer
  checking the installed version's sources or the docs at <https://docs.korge.org>.
- Run during development with `./gradlew runJvmAutoreload` (hot-reloads the current Scene on
  every code change). See `references/project-setup.md`.

## Getting unstuck

Official docs: <https://docs.korge.org>. Samples (great for real, compiling code):
<https://github.com/korlibs/korge-samples>. Community: Discord at <https://discord.korge.org>.
When an API in these references looks off for the version in use, trust the installed
version's sources over the doc text.