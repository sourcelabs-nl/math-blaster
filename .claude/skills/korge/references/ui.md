# UI components and layouts

KorGE ships ready-made UI widgets and layout containers, all DSL builders on a `Container`. They
work well for menus, HUDs, and tools. For in-world game objects, prefer plain views
(`references/views.md`).

## Common widgets

```kotlin
uiButton("Play") {
    onClick { /* ... */ }       // or .clicked { }
    width = 190f
    bgColorOut  = MaterialColors.AMBER_500
    bgColorOver = MaterialColors.AMBER_800
    textColor   = MaterialColors.BLUE_900
    background.radius = RectCorners(16f, 0f, 12f, 4f)
}
uiButton(icon = resourcesVfs["korge.png"].readBitmapSlice())

uiCheckBox(text = "Sound", checked = true)

val group = UIRadioButtonGroup()
uiVerticalStack {
    uiRadioButton(text = "Easy",   group = group)
    uiRadioButton(text = "Hard",   group = group)
}

uiComboBox(size = Size(160f, 32f), items = listOf("A", "B", "C"), selectedIndex = 0)

uiText("Hello World!")                       // plain single-line text wrapper
uiTextInput("Type here")                      // editable input

textBlock(                                    // rich text: HTML, wrapping, ellipsis
    RichTextData.fromHTML(
        "hello <b>world</b>, <font color=red>this</font> is long text",
        RichTextData.Style.DEFAULT.copy(font = DefaultTtfFontAsBitmap),
    ),
    size = Size(100f, 48f),
)

uiImage(Size(120f, 32f), slice, scaleMode = ScaleMode.FIT, contentAnchor = Anchor.CENTER)
uiProgressBar(size = Size(256, 8), current = 75f, maximum = 100f)
uiWindow                                       // draggable/resizable/closeable window
uiTreeView(...)                                // collapsible tree
uiBreadCrumb(listOf("home", "level", "1"))
uiEditableNumber(10.0, min = 0.0, max = 100.0)
```

### UISlider
```kotlin
uiSlider(value = 50.0, min = -50.0, max = 50.0, step = 1.0) {
    showTooltip = true
    marks = true
    decimalPlaces = 2
    textTransformer = { "$itº" }
    styles {
        uiSelectedColor   = MaterialColors.RED_600
        uiBackgroundColor = MaterialColors.BLUE_50
    }
    changed { println("slider = $it") }       // or onChange.add { }
}
```

### UIMaterialLayer
A rounded rectangle with border, drop shadow, and background — handy for cards/panels.
```kotlin
uiMaterialLayer().also {
    it.size = Size(120f, 60f)
    it.radius = RectCorners(16f, 8f)
    it.bgColor = Colors.GREEN
    it.borderColor = Colors.PURPLE
    it.borderSize = 4f
    it.shadowColor = Colors.BLUE
    it.shadowRadius = 10f
}
```

## Layout containers

```kotlin
uiVerticalStack(padding = 4f) {
    uiButton("one"); uiButton("two")
    uiSpacing(Size(0, 10))
    uiButton("three")
}

uiHorizontalStack(padding = 4f) {
    uiButton("left"); uiButton("right")
}

uiGridFill(cols = 3, rows = 3) {
    for (n in 0 until 9) uiButton("$n")
}

uiScrollable(Size(300, 300)) {
    image(resourcesVfs["big.png"].readBitmapSlice())
}

// Lazily-rendered long list (only visible items are built):
uiScrollable(Size(160, 120)) {
    uiVerticalList(object : UIVerticalList.Provider {
        override val fixedHeight: Float? get() = 16f
        override val numItems: Int get() = 1000
        override fun getItemHeight(index: Int) = 16f
        override fun getItemView(index: Int, vlist: UIVerticalList): View =
            TextBlock(RichTextData.fromHTML("Element $index"))
    }, width = 160f)
}
```

## Styling (inherited)

Attach styles on a `uiContainer`; all descendants inherit them.

```kotlin
uiContainer {
    styles {
        textColor = Colors.RED
        textSize  = 32f
    }
    uiText("Styled text")
}
```
