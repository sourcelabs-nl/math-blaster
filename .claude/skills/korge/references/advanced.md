# Advanced: resolution/scaling, persistent storage, testing

## Resolution and virtual size

You design against a fixed **virtual** size; KorGE scales it to the real window. You almost never
work in raw window pixels.

```kotlin
suspend fun main() = Korge(width = 1280, height = 720, virtualWidth = 640, virtualHeight = 480) { }
// or via Module:
object MyModule : Module() {
    override val size = SizeInt(640, 480)        // virtual
    override val windowSize = SizeInt(1280, 720) // window
}
```

`width`/`height` are the window size (ignored on fullscreen/web where JS can't resize the window).
`virtualWidth`/`virtualHeight` are your in-game coordinate space: place something at `639,479` in
a 640×480 virtual space and it lands at the bottom-right regardless of window size.

### Aspect ratio handling
Configured by `scaleAnchor`, `scaleMode`, `clipBorders`:
```kotlin
Korge(scaleAnchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.SHOW_ALL, clipBorders = true) { }
```
- `SHOW_ALL` + `clipBorders = true` → letterbox (black bars), content centered, aspect kept.
- `SHOW_ALL` + `clipBorders = false` → content centered & fills space, border pixels become visible.

### Keeping views anchored across resizes — `dockedTo`
```kotlin
val hud = container {
    solidRect(32, 32, Colors.RED).anchor(1.0, 0.0)
}
hud.dockedTo(anchor = Anchor.TOP_RIGHT, scaleMode = ScaleMode.NO_SCALE)
```
This auto-updates position on resize/rotation, so you usually don't need a manual resize handler.

### Size query properties on `Views`
```kotlin
views.nativeWidth / nativeHeight            // window size
views.virtualWidth / virtualHeight          // your declared virtual size
views.actualVirtualWidth / actualVirtualHeight   // >= virtual; has window's aspect ratio
views.actualVirtualLeft / actualVirtualTop  // border gap when not anchored top-left
views.virtualLeft / virtualTop / virtualRight / virtualBottom
```

## Persistent storage / preferences

Two options for data that survives restarts.

### Files: `applicationDataVfs`
A per-app local VFS for arbitrary files.
```kotlin
applicationDataVfs["save.json"].writeString(json)
val json = applicationDataVfs["save.json"].readString()
```
Pair this with kotlinx.serialization (enable `serializationJson()` in `build.gradle.kts`) for
structured save data.

### Key/value: `NativeStorage`
Behaves like an auto-persisted `MutableMap<String, String>` (think Android SharedPreferences).
```kotlin
val storage = views.storage
storage["coins"] = "100"
val coins = storage.getOrNull("coins")          // String?
"coins" in storage                               // contains check
storage.remove("coins"); storage.removeAll()
```

Typed `StorageKey` wrappers (also usable as delegated properties):
```kotlin
val highScore = storage.itemInt("highScore", default = 0)
highScore.value = 9000
println(highScore.value); println(highScore.isDefined)

var soundOn by storage.itemBool("soundOn", default = true)   // delegated property
soundOn = false
```
Also `itemString`, `itemDouble`.

## Testing

KorGE makes headless, fast tests possible — tweens and timed logic run almost instantly.

### Suspend-only tests
```kotlin
class IoTest {
    @Test fun test() = suspendTest {
        assertEquals("world", resourcesVfs["hello.txt"].readString())
    }
}
```

### View / scene tests — extend `ViewsForTesting`
```kotlin
class MyViewsTest : ViewsForTesting() {
    @Test fun test() = viewsTest {                    // this: Stage
        val rect = solidRect(100, 100, Colors.RED)
        val log = arrayListOf<String>()
        rect.onClick { log += "clicked" }

        rect.simulateClick()
        assertEquals(true, rect.isVisibleToUser())
        tween(rect::x[-102], time = 10.seconds)        // runs instantly
        assertEquals(false, rect.isVisibleToUser())
        assertEquals(listOf("clicked"), log)
    }
}
```
Helpers: `mouseMoveTo`, `mouseDown`, `mouseUp`, `View.simulateClick/simulateOver/simulateOut`,
`View.isVisibleToUser()`. Scene tests: `sceneTest<MyScene>(module) { ... }`, with the option to
override injector bindings for mocking.

### Recommended architecture: separate logic from presentation
Model the game as plain data and **pure** state transitions; render those with views/tweens.
Then the core is testable with no window at all:
```kotlin
data class State(/* ... */)
sealed class UserAction { data class RequestMove(/* ... */) : UserAction() }
data class Transition(val prev: State, val next: State, val operations: List<Operation>)

fun applyUserAction(state: State, action: UserAction): Transition { /* pure */ }
suspend fun SContainer.animateTransition(t: Transition) { /* views + tweens */ }

@Test fun testMove() {                                 // pure, fast, no views
    val t = applyUserAction(State(), UserAction.RequestMove(/* ... */))
    /* assert on t */
}
@Test fun testAnimation() = viewsTest { /* verify the visual side */ }
```

The flow to keep in mind:
`User Interactions → User Actions → State Transitions → View Animations`.
