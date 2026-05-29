# Project setup, build, and deployment

KorGE is configured through its **Gradle plugin** (`com.soywiz.korge`). Do not attempt to
build KorGE multiplatform targets with Maven — the plugin, Kotlin/Multiplatform targets, and
packaging tasks are Gradle-only.

## Recommended start: KorGE Forge IDE

The official, easiest path is **KorGE Forge** — a customized IntelliJ IDE with a `New Project`
wizard offering starter kits and full-game showcases, plus a `runJvmAutoreload` run config and
a KorGE Store for optional modules (Tiled, LDtk, SWF, Box2D, etc.). Install instructions and
the store live at <https://forge.korge.org> and <https://store.korge.org>. For an agent editing
files, the manual project layout below is what matters.

## Minimal project layout

Four files (plus Gradle 8.5+) are enough:

### `gradle/libs.versions.toml`

```toml
[plugins]
korge = { id = "com.soywiz.korge", version = "5.4.0" }   # check latest at docs.korge.org
```

### `settings.gradle.kts`

This template extracts the KorGE version from `libs.versions.toml` (version catalogs aren't
available in the settings buildscript yet):

```kotlin
pluginManagement {
    repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() }
}

buildscript {
    val libsTomlFile = File(this.sourceFile?.parentFile, "gradle/libs.versions.toml").readText()
    var plugins = false
    var version = ""
    for (line in libsTomlFile.lines().map { it.trim() }) {
        if (line.startsWith("#")) continue
        if (line.startsWith("[plugins]")) plugins = true
        if (plugins && line.startsWith("korge") && Regex("^korge\\s*=.*").containsMatchIn(line))
            version = Regex("version\\s*=\\s*\"(.*?)\"").find(line)?.groupValues?.get(1)
                ?: error("Can't find korge version")
    }
    if (version.isEmpty()) error("Can't find korge version in $libsTomlFile")
    repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() }
    dependencies {
        classpath("com.soywiz.korge.settings:com.soywiz.korge.settings.gradle.plugin:$version")
    }
}

apply(plugin = "com.soywiz.korge.settings")
```

### `build.gradle.kts`

```kotlin
import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "com.sample.demo"

    // targetAll()          // enable every target at once
    // targetDefault()      // enable based on properties/env vars
    targetJvm()             // or selectively enable targets:
    targetJs()
    targetDesktop()
    targetIos()
    targetAndroid()
    targetWasm()

    serializationJson()     // opt-in kotlinx.serialization JSON support
}

dependencies {
    // add("commonMainApi", project(":deps"))   // kproject / extra modules
}
```

### `src/commonMain/kotlin/main.kt`

The entry point must be a **`suspend fun main()` with no arguments**. On Android/iOS the KorGE
plugin calls it from an `Activity` / `ViewController` for you.

```kotlin
suspend fun main() = Korge {
    sceneContainer().changeTo { MyScene() }
}

class MyScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        solidRect(100, 100, Colors.RED)
    }
}
```

Platform-specific sources go in sibling source sets: `src/jvmMain/kotlin`, `src/androidMain/kotlin`,
`src/jsMain/kotlin`, `src/iosMain/kotlin`, etc.

## The `korge { }` extension (app metadata)

Configure application identity and packaging in `build.gradle.kts`:

```kotlin
korge {
    id = "com.unknown.unknownapp"
    version = "0.0.1"
    name = "unnamed"
    exeBaseName = "app"
    description = "description"
    orientation = Orientation.DEFAULT
    icon = File(rootDir, "icon.png")
    fullscreen = true
    backgroundColor = 0xff000000.toInt()
    jvmMainClassName = "MainKt"
    gameCategory = GameCategory.ACTION
    config("MYPROP", "MYVALUE")        // exposed at runtime
    // admob(ADMOB_APP_ID)             // monetization plugins, etc.
}
```

## Running and building

Use `gradlew.bat` instead of `./gradlew` on Windows.

- **`./gradlew runJvmAutoreload`** — the main dev task. Recompiles and hot-swaps the current
  Scene's code on every change. This is the run config the Forge wizard creates.
- **`./gradlew runJvm`** — run on the JVM (run in debug mode from IntelliJ to debug; press
  **F7** while running to toggle view bounds/debug overlay).
- Web / mobile / native packaging tasks are added per enabled target. See
  <https://docs.korge.org/targets/> for the full task list (`runJs`, `packageJvmFatJar`,
  iOS/Android build tasks, etc.).
