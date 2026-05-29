# Views: the display tree and standard views

A `View` is anything drawable. A `Container` is a `View` that holds children. KorGE renders the
tree back-to-front (painter's algorithm): earlier-created siblings draw behind later ones.

## View properties

Transform / appearance (all mutable, animatable):

- `x`, `y` — position (Double)
- `scaleX`, `scaleY`, plus `scale` (sets both)
- `rotation` — an `Angle` (`45.degrees`)
- `skewX`, `skewY`
- `visible` (Boolean), `alpha` (Double 0..1)
- `colorMul` — RGBA tint
- `blendingMode` — `NORMAL`, `ADD`, `MULTIPLY`, `SUBTRACT`
- `speed` — time multiplier for this view's updaters/tweens (1.0 normal; affects descendants)
- `zIndex` — reordering score within the parent (see below)

Computed: `globalX`, `globalY`, `globalMatrix`, `globalBounds`.

## Positioning / transform extension helpers (chainable)

```kotlin
view.xy(100, 50)
view.position(100, 50)
view.scale(2.0)
view.rotation(30.degrees)
view.anchor(Anchor.CENTER)          // set the view's pivot/anchor
view.alpha(0.5)
view.visible(false)
```

## zIndex ordering within a container

`zIndex` is a Float, default `0f`, **local to the parent**. Higher = front, lower = back,
relative to siblings at `0f`. To reorder views you must give them a common parent.

```kotlin
boxGreen.zIndex = +1f   // moves to front
boxBlue.zIndex  = -1f   // moves to back
```

## Adding / removing / finding children

A `Container` exposes: `addChild`, `addChildAt(view, index)`, `removeChild`, `removeChildren`,
`swapChildren`, `getChildAt(index)`, `getChildByName(name)`, `getChildIndex(view)`, and the
`children` list. Operator shortcuts: `container += view`, `container -= view`. Generic:
`view.addTo(container)` and `view.removeFromParent()`.

## Standard views (DSL builder + class)

Each builder takes the parent `Container` as receiver, adds the view, and returns it. A trailing
lambda configures the new view (it as receiver).

### Container / FixedSizeContainer
```kotlin
val group = container { /* children */ }                  // size derives from children
val panel = fixedSizeContainer(width = 200, height = 120) { }
```

### SolidRect / RoundRect / Circle / Ellipse
```kotlin
solidRect(width = 100, height = 100, color = Colors.RED)
solidRect(Size(100, 100), Colors.RED)                     // Size overload
roundRect(width = 100, height = 60, rx = 8.0, color = Colors.WHITE)
circle(radius = 16.0, color = Colors.WHITE)
ellipse(radiusX = 20.0, radiusY = 12.0, color = Colors.WHITE)
```

### Image
The most common view in 2D games. Construct from a `Bitmap` or `BmpSlice`. Textures are
uploaded/freed on the GPU automatically.
```kotlin
val bmp = resourcesVfs["player.png"].readBitmap()
image(bmp) {
    anchor(Anchor.CENTER)
    smoothing = false        // false = nearest-neighbor (crisp pixel art); true = linear (default)
    xy(100, 100)
}
```

### Text
Renders text with a `BitmapFont`; supports a small subset of HTML for formatting.
```kotlin
val t = text("Score: 0", textSize = 32.0, color = Colors.WHITE, font = myFont)
t.text = "Score: 10"        // update later
// View?.setText("...") / setHtml("...") update matching descendants
```
See `references/resources.md` for fonts. For rich text/wrapping prefer `textBlock` /
`UIText` (see `references/ui.md`).

### Graphics (vector drawing)
Implements the `VectorBuilder` API; rasterizes vector shapes to an image.
```kotlin
graphics {
    fill(Colors.RED) {
        rect(0, 0, 100, 100)
        circle(Point(150, 50), 30.0)
    }
    stroke(Colors.WHITE, info = StrokeInfo(thickness = 2.0)) {
        line(Point(0, 0), Point(100, 100))
    }
}
```

### NinePatch
Like an Image, but preserves corner/edge sizes when stretched (panels, buttons, bubbles).
```kotlin
ninePatch(tex, width = 200.0, height = 80.0, left = 8.0, top = 8.0, right = 8.0, bottom = 8.0)
// Or from an IntelliJ-compatible NinePatchBitmap32:
ninePatch(resourcesVfs["panel.9.png"].readNinePatch(), width = 200.0, height = 80.0)
```

### Sprite
Frame-based sprite animation — see the sprites section in `references/resources.md`.

### Camera / CameraContainer
```kotlin
cameraContainer(content = {
    // world contents
}).apply {
    follow(player)
    // tweenCamera(...), updateCamera { setTo(rect) }
}
```

### ScaleView
A fixed-size container rendered at native size to a texture, then scaled (filtered or pixelated)
— useful for retro games.
```kotlin
scaleView(width = 320, height = 180, scale = 2.0, filtering = false) { /* world */ }
```

### Mesh
Raw triangles / triangle strips for custom rendering and mesh deforms.

## Alignment & centering

Family of `align___To___Of` extensions. `alignLeftToLeftOf(other)` aligns this view's left edge
to `other`'s left edge; `alignLeftToRightOf(other)` places this view to the right of `other`,
etc. All take an optional `padding`. Discover the full set via IDE autocomplete.

Centering helpers:
```kotlin
view.centerOn(other)
view.centerXOn(other); view.centerYOn(other)
view.centerOnStage(); view.centerXOnStage()
view.centerBetween(x1, y1, x2, y2)
```

## Components (attach behavior to a view)

A `Component` is attached to a `View` and lives/dies with it. Common interfaces:
`UpdateComponent { update(ms) }`, `UpdateComponentWithViews`, `ResizeComponent { resized(views, w, h) }`,
`EventComponent`, `MouseComponent`, `KeyComponent`, `GamepadComponent`, `TouchComponent`.

```kotlin
class DockingComponent(override val view: View, var anchor: Anchor) : ResizeComponent {
    override fun resized(views: Views, width: Int, height: Int) {
        view.x = views.actualVirtualLeft + views.actualVirtualWidth * anchor.sx
        view.y = views.actualVirtualTop  + views.actualVirtualHeight * anchor.sy
        view.invalidate(); view.parent?.invalidate()
    }
}
fun <T : View> T.dockedTo(anchor: Anchor) = DockingComponent(this, anchor).attach()
```

(Most apps don't write components directly — `addUpdater`/`onClick`/`onStageResized` cover the
common cases. Use components when you want reusable behavior tied to a view's lifecycle.)

## Screenshots / render-to-bitmap

```kotlin
val bmp: Bitmap32 = stage.renderToBitmap(stage.views)
val viewBmp = someView.renderToBitmap(views)
```
