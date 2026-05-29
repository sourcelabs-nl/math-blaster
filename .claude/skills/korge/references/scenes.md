# Entry point, Scenes, transitions, navigation, injector

## Entry point (`Korge { }`)

The basic form, with the `Stage` as receiver:

```kotlin
suspend fun main() = Korge {            // this: Stage
    solidRect(100, 100, Colors.RED)
}
```

Set resolution and window options as `Korge(...)` arguments:

```kotlin
suspend fun main() = Korge(
    width = 1280, height = 720,         // window size (ignored on fullscreen/web)
    virtualWidth = 640, virtualHeight = 480,
    backgroundColor = Colors["#2b2b2b"],
    scaleMode = ScaleMode.SHOW_ALL,     // letterboxing behavior
    scaleAnchor = Anchor.MIDDLE_CENTER,
    clipBorders = true,
) { /* Stage receiver */ }
```

Module-based entry point (uses the dependency injector and a declared main scene):

```kotlin
suspend fun main() = Korge(Korge.Config(module = MyModule))

object MyModule : Module() {
    override val mainScene = MyScene1::class
    override val size = SizeInt(640, 480)        // virtual size
    override val windowSize = SizeInt(1280, 720) // window size
    override suspend fun Injector.configure() {
        mapInstance(MyDependency("HELLO WORLD"))
        mapPrototype { MyScene1(get()) }
    }
}
```

## Scenes

A `Scene` is a controller for one screen. Override the lifecycle methods you need; receivers
are `SContainer` (an alias for the scene's container).

```kotlin
class MyScene : Scene() {
    override suspend fun SContainer.sceneInit() {
        // BLOCKING. Runs before the scene is shown. Load/await resources here;
        // nothing is displayed until this returns. No need to call super.
    }
    override suspend fun SContainer.sceneMain() {
        // NON-BLOCKING. Main body: add views, attach events, start tweens.
        // May suspend forever; its job is cancelled on scene destroy.
    }
}
```

For simple scenes, override only `sceneMain`.

### Full lifecycle hooks

```kotlin
override suspend fun SContainer.sceneInit() { }   // BLOCK: setup before show
override suspend fun SContainer.sceneMain() { }   // DON'T BLOCK: main loop body
override suspend fun sceneAfterInit() { }         // DON'T BLOCK: after transition completes
override suspend fun sceneBeforeLeaving() { }     // BLOCK: on old scene before transition
override suspend fun sceneDestroy() { }           // BLOCK: after transition, not visible
override suspend fun sceneAfterDestroy() { }      // DON'T BLOCK: scene job cancelled here
override fun onSizeChanged(size: Size) { }        // scene resized
```

## SceneContainer and changing scenes

A `SceneContainer` is itself a `View`, so it lives in the scene graph:

```kotlin
suspend fun main() = Korge {
    val sceneContainer = sceneContainer()           // or SceneContainer().addTo(this)
    sceneContainer.changeTo { MainMenuScene() }
}
```

Inside a scene you always have a `sceneContainer` reference to route onward:

```kotlin
uiButton("Play") { onClick { sceneContainer.changeTo { GameScene() } } }
```

### Passing parameters to scenes

Add them to the constructor:

```kotlin
class GameScene(val service: MyService, val levelName: String) : Scene()

sceneContainer.changeTo { GameScene(service, "level-1") }
```

With many same-typed params, wrap them in a data class:

```kotlin
data class GameParams(val levelName: String, val seed: Int)
class GameScene(val service: MyService, val params: GameParams) : Scene()
sceneContainer.changeTo { GameScene(service, GameParams("level-1", 42)) }
```

## Transitions

```kotlin
sceneContainer.changeTo<IngameScene>(
    transition = MaskTransition(TransitionFilter.Transition.CIRCULAR, reversed = false, filtering = true),
    // transition = AlphaTransition,                // simple cross-fade (singleton, no params)
    time = 0.5.seconds,
)
```

`MaskTransition` blends per pixel along a pattern. Built-in patterns:
`TransitionFilter.Transition.{VERTICAL, HORIZONTAL, DIAGONAL1, DIAGONAL2, CIRCULAR, SWEEP}`,
or supply a custom greyscale `Bitmap` via `TransitionFilter.Transition(bmp)`.

## Special scene base classes

`ScaledScene` / `PixelatedScene` give the scene a fixed effective size that is rendered to a
texture and scaled into the available space (smooth vs nearest-neighbor). Great for retro
pixel art or guaranteeing layout at a known resolution:

```kotlin
class GameScene : PixelatedScene(sceneWidth = 320, sceneHeight = 180) {
    override suspend fun SContainer.sceneMain() { /* design against 320x180 */ }
}
```

## Navigation stack (injector-based)

With the injector you get a back/forward stack:

```kotlin
injector.mapPrototype { LevelScene(get()) }

sceneContainer.pushTo<LevelScene>(GameParams("level-1", 1))
sceneContainer.pushTo<LevelScene>(GameParams("level-2", 2))
sceneContainer.back()                  // back to level-1
sceneContainer.forward()               // forward to level-2
println(sceneContainer.navigationEntries)
```

This requires the injector because `back`/`forward` reconstruct scenes from their injected
parameters.

## Dependency injector (Korinject)

KorGE bundles an async DI container, reachable as `injector` from the stage/scene/module.

```kotlin
injector.mapInstance(Config(...))             // a specific existing instance
injector.mapSingleton { MyService(get()) }    // one shared lazily-created instance
injector.mapPrototype { MyScene(get()) }      // a fresh instance each request

sceneContainer.changeTo<MyScene>()            // resolves MyScene + its deps from the injector
```

`get()` inside a mapping resolves another dependency by type. The injector is optional — you
can pass dependencies through constructors manually if you prefer.
