package ovh.litapp.keepscore

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The user interface is built with Jetpack Compose, so the resource tree must not grow XML layouts
 * or option menus back.
 *
 * <p>This replaces the AndroidX migration guard that used to scan layout XML for removed support
 * classes: with no layout left to scan, the invariant worth protecting is that none comes back.
 * Layouts and menus fail silently at runtime (a screen renders nothing, a menu item disappears), so
 * they cannot be caught by compiling.
 */
class NoLegacyLayoutUiTest {

    @Test
    fun noLayoutOrMenuXmlRemains() {
        val res = File("src/main/res")
        // Detects running from the wrong directory rather than silently finding no files.
        assertTrue(
            "expected to run from the app module directory: " + res.absolutePath,
            res.isDirectory,
        )

        val legacyUiFiles = Files.walk(res.toPath()).use { paths ->
            paths.filter(::isLayoutOrMenuXml).map { res.toPath().relativize(it).toString() }.toList()
        }

        assertTrue(
            "layout and menu XML must stay deleted; found: $legacyUiFiles",
            legacyUiFiles.isEmpty(),
        )
    }

    @Test
    fun noSourceInflatesLayoutOrMenuResources() {
        val javaMain = File("src/main/java")
        assertTrue(
            "expected to run from the app module directory: " + javaMain.absolutePath,
            javaMain.isDirectory,
        )

        val inflations = Files.walk(javaMain.toPath()).use { paths ->
            paths
                .filter { it.fileName.toString().endsWith(".kt") || it.fileName.toString().endsWith(".java") }
                .flatMap { path ->
                    val source = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                    val hits = INFLATION_REFERENCES.findAll(source).map { it.value }.toList()
                    hits.stream().map { "${javaMain.toPath().relativize(path)}: $it" }
                }
                .toList()
        }

        assertFalse(
            "screens must be Compose, not inflated XML; found: $inflations",
            inflations.isNotEmpty(),
        )
    }

    private fun isLayoutOrMenuXml(path: Path): Boolean {
        val fileName = path.fileName.toString()
        if (!fileName.endsWith(".xml")) {
            return false
        }
        val directory = path.parent?.fileName?.toString() ?: return false
        return directory.startsWith("layout") || directory == "menu"
    }

    private companion object {
        /** `R.layout.x` and `R.menu.x` references, the only way XML UI is reached. */
        private val INFLATION_REFERENCES = Regex("""\bR\.(layout|menu)\.[a-zA-Z0-9_]+""")
    }
}
