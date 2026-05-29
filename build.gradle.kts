import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "nl.sourcelabs.mathblaster"
    name = "Math Blaster"
    jvmMainClassName = "MainKt"

    targetJvm()
    targetWasmJs()
}

// mavenLocal serves an incomplete kotlin-test (POM only, no Gradle .module metadata), so its
// JVM test-framework capability variant is invisible and jvmTest fails to resolve. KorGE adds
// mavenLocal to the project repositories; drop it so these deps resolve from mavenCentral.
repositories.removeIf { it.name == "MavenLocal" }

kotlin {
    sourceSets {
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
