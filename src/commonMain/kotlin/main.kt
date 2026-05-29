import game.scene.GameScene
import game.view.RetroTheme
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer

suspend fun main() = Korge(
    virtualWidth = 960,
    virtualHeight = 600,
    backgroundColor = RetroTheme.space,
) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { GameScene() }
}
