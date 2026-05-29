# Input: mouse, keyboard, touch, gamepad, drag & drop

Two complementary styles:

1. **Poll state** — read `input.*` inside an updater (`addUpdater`). Best for continuous
   movement and "is this key held".
2. **Events** — attach handlers to a view (`onClick`, `view.keys { }`). Best for discrete
   actions. Events fire only while the view is attached to the stage.

`input` is the per-frame input snapshot, reachable as `views.input` or just `input` inside a
scene/view context.

## Mouse / touch

### State (poll)
```kotlin
view.addUpdater {
    val pos: Point = input.mouse
    val buttons: Int = input.mouseButtons       // bit flags of pressed buttons
    val touches: List<Touch> = input.activeTouches
    val n = input.activeTouches.size
}
```

### Events
Available mouse events: `click`, `over` (hover enter), `out`, `down`, `downFromOutside`, `up`,
`upOutside`, `upAnywhere`, `move`, `moveAnywhere`, `moveOutside`, `exit`.
```kotlin
view.mouse {
    click { /* ... */ }
    over  { /* hover */ }
    out   { /* ... */ }
    down  { /* ... */ }
    up    { /* ... */ }
}
// Shortcut (suspend block):
view.onClick { /* can suspend, e.g. await a tween or change scene */ }
```

## Keyboard

### State (poll) — typical for movement
```kotlin
view.addUpdater { dt ->
    val scale = dt / 16.milliseconds                      // normalize to ~60fps frame
    if (input.keys[Key.LEFT])  x -= 2.0 * scale           // held (same as .pressing)
    if (input.keys.pressing(Key.RIGHT)) x += 2.0 * scale
    if (input.keys.justPressed(Key.ESCAPE)) views.gameWindow.close(0)  // one frame on press
    if (input.keys.justReleased(Key.ENTER)) println("released")        // one frame on release
}
```

### Events
```kotlin
view.keys {
    down { e -> }                       // any key down
    up   { e -> }
    down(Key.LEFT)        { e -> }       // specific key down (may repeat per platform)
    up(Key.LEFT)          { e -> }
    justDown(Key.LEFT)    { e -> }       // fires once until released & pressed again
    downFrame(Key.LEFT)   { e -> }       // every frame while held
    downFrame(Key.LEFT, 16.milliseconds) { e -> }  // throttled to interval
    downRepeating(Key.LEFT) { e -> }     // UI-style auto-repeat with acceleration
}
// Shortcuts (suspend blocks):
view.onKeyDown { e -> }
view.onKeyUp   { e -> }
```

## Gamepad

### State
```kotlin
view.addUpdater {
    val pads = input.connectedGamepads
    val p0 = input.gamepads[0]
    val start: Boolean = p0[GameButton.START]
    val leftStick: Point = p0[GameStick.LEFT]
}
```

### Events
```kotlin
view.gamepad {
    connected    { playerId -> }
    disconnected { playerId -> }
    stick(playerId = 0, GameStick.LEFT) { x, y -> }
    button(playerId = 0) { pressed, button, value -> }
    down(playerId = 0, GameButton.BUTTON0) { }
}
```

## Window resize

```kotlin
view.onStageResized { width, height -> }
view.onEvent(ViewsResizedEvent) { }
// Low level, on the stage:
stage.addEventListener<ReshapeEvent> { e -> /* e.x, e.y, e.width, e.height */ }
```
Prefer attaching to a view so handlers are cleaned up when the view detaches (avoids leaks).
See also the `dockedTo` helper in `references/advanced.md` for keeping views anchored on resize.

## Drag & drop files

```kotlin
val overlay = solidRect(Size(width, height), Colors.RED).visible(false)
onDropFile {
    when (it.type) {
        DropFileEvent.Type.START -> overlay.visible = true
        DropFileEvent.Type.END   -> overlay.visible = false
        DropFileEvent.Type.DROP  -> launchImmediately {
            it.files?.firstOrNull()?.let { f -> image(f.readBitmap()).size(Size(width, height)) }
        }
    }
}
// Or raw:
onEvents(*DropFileEvent.Type.ALL) { println(it.type) }
```

## Dragging views

```kotlin
val box = solidRect(Size(100, 100), Colors.RED)
val closeable = box.draggableCloseable()        // returns a Closeable to stop dragging later

// Constrain to one axis / custom handling:
box.draggableCloseable(selector = box, autoMove = false) { info: DraggableInfo ->
    info.view.x = info.viewNextXY.x              // only move on X
}
```
