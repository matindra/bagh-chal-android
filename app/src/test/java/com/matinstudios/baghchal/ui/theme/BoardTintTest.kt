package com.matinstudios.baghchal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.matinstudios.baghchal.data.BoardTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTintTest {

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun `wood keeps the app's own palette and every other board has a tint`() {
        assertNull(BoardTheme.WOOD.tint)
        assertTrue(BoardTheme.entries.filter { it != BoardTheme.WOOD }.all { it.tint != null })
    }

    @Test
    fun `text stays readable on every tinted board, light and dark`() {
        for (theme in BoardTheme.entries) {
            val tint = theme.tint ?: continue
            for (dark in listOf(false, true)) {
                val s = (if (dark) darkColorScheme() else lightColorScheme()).tinted(tint, dark)
                val where = "$theme ${if (dark) "dark" else "light"}"
                assertTrue(contrast(s.onBackground, s.background) >= 7f, "$where body text")
                assertTrue(contrast(s.onSurface, s.surfaceContainerHighest) >= 7f, "$where text on cards")
                assertTrue(contrast(s.onSurfaceVariant, s.surfaceContainer) >= 4.5f, "$where secondary text")
                assertTrue(contrast(s.onTertiaryContainer, s.tertiaryContainer) >= 4.5f, "$where text on accent cards")
                assertTrue(contrast(s.onTertiary, s.tertiary) >= 4.5f, "$where text on accent buttons")
            }
        }
    }

    @Test
    fun `tiger and goat colours never change`() {
        val base = lightColorScheme(primary = TigerOrange40, secondary = Meadow40)
        for (theme in BoardTheme.entries) {
            val s = base.tinted(theme.tint ?: continue, dark = false)
            assertEquals(TigerOrange40, s.primary)
            assertEquals(Meadow40, s.secondary)
        }
    }
}
