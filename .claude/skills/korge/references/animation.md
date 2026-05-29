# Animation: updaters, tweens, the animator, easings

KorGE offers several layers. From lowest to highest level:

## Per-frame updaters

`addUpdater` runs every frame with a delta time (`TimeSpan`). **Always scale motion by the
delta** so speed is frame-rate independent.

```kotlin
view.addUpdater { dt ->                 // dt: TimeSpan since last frame
    val scale = dt / 16.66666.milliseconds
    x += 2.0 * scale
}
```

`addFixedUpdater` runs a fixed number of times per second (may stutter slightly):
```kotlin
view.addFixedUpdater(60.timesPerSecond) { x++ }
```

A raw coroutine loop also works for state-machine-like logic (can stutter):
```kotlin
launchImmediately {
    while (true) { view.x++; delay(16.milliseconds) }
}
```

## Tweens

`View.tween` interpolates properties over time, integrated with coroutines (it suspends until
done). Specify properties with **bound callable references** and the `[from, to]` / `[to]`
indexing operator.

> Import `korlibs.korge.tween.get` for the `view::x[...]` syntax to compile.

```kotlin
// Interpolate x from 10 to 100 over 1 second:
view.tween(view::x[10.0, 100.0], time = 1.seconds)

// Interpolate to 100 starting from the current x:
view.tween(view::x[100.0], time = 1.seconds)

// Multiple properties, with easing:
view.tween(
    view::x[300.0],
    view::y[0.0, 200.0],
    view::rotation[90.degrees],
    time = 1.seconds,
    easing = Easing.EASE_IN_OUT,
)
```

Per-property timing via `V2` chain extensions (`delay`, `duration`, `easing`):
```kotlin
view.tween(
    view::x[100.0].delay(100.milliseconds).duration(500.milliseconds).easing(Easing.EASE_IN_OUT_QUAD),
    view::y[0.0, 200.0].delay(50.milliseconds),
    time = 1.seconds,
)
```

The tween attaches as a component to the view, so the view must be on the stage (or manually
updated). `View.speed` on the view or an ancestor scales the tween — you can even tween
`view::speed` for time effects.

## Animator (sequences & parallel groups)

An `animator` is attached to a view and queues `sequence`/`parallel` blocks of tween-like
actions. More flexible than a single tween for choreography.

```kotlin
val anim = animator(parallel = false)   // sequence by default
anim.sequence {
    parallel {
        alpha(view1, 0.5)
        alpha(view2, 0.5)
    }
    parallel {
        moveBy(view1, x = +100, y = +100)
        moveBy(view2, x = +100, y = +100)
    }
    block { println("done choreographing") }
}
```

Common operations (most accept `time =` and `easing =`):
```kotlin
anim.tween(view::x[0, 100], time = 1.seconds)
anim.moveTo(view, x = 100, y = 200)
anim.moveBy(view, x = +10, y = +20)
anim.moveToWithSpeed(view, x = 200, y = 200, speed = 100.0)
anim.moveInPath(view, path, time = 2.seconds)        // follow a buildVectorPath
anim.rotateBy(+30.degrees); anim.rotateTo(90.degrees)
anim.scaleTo(view, 2.0, 2.0)
anim.alpha(view, 0.5); anim.hide(); anim.show()
anim.wait(2.seconds)
anim.block { /* sync code at this point */ }
anim.removeFromParent(view)
```

Behavior: in a **sequence** animator, new actions queue after current ones; in a **parallel**
animator they start immediately. `view.simpleAnimator` is a lazy parallel animator singleton
for quick one-offs:
```kotlin
uiButton("Move").clicked { view.simpleAnimator.moveBy(view, x = +100.0) }
```

Finish control:
- `anim.cancel()` — stop now, leave properties where they are.
- `anim.complete()` — jump everything pending to final values and run remaining blocks.
- `anim.awaitComplete()` — suspend until the queue drains.
- `anim.onComplete.once { }` — callback when finished.

## Easings

`Easing` provides the standard set. Examples:
`Easing.LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`, `EASE_OUT_IN`,
`EASE_IN_QUAD`/`EASE_OUT_QUAD`/`EASE_IN_OUT_QUAD`,
`EASE_IN_BACK`/`EASE_OUT_BACK`/`EASE_IN_OUT_BACK`,
`EASE_IN_ELASTIC`/`EASE_OUT_ELASTIC`, `EASE_IN_BOUNCE`/`EASE_OUT_BOUNCE`. You can also define
custom easings.

## SWF / skeletal animation

SWF and skeletal/mesh-deform animations are supported via separate store modules
(`korge-swf`, Dragonbones/Spine). Add them from the KorGE Store when needed.
