package com.matinstudios.baghchal.ui.board

import androidx.annotation.DrawableRes
import androidx.compose.runtime.staticCompositionLocalOf
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.PieceSet

/** The tiger and goat drawables of one [PieceSet]. */
data class PieceArt(@DrawableRes val tiger: Int, @DrawableRes val goat: Int)

val PieceSet.art: PieceArt
    get() = when (this) {
        PieceSet.CLASSIC -> PieceArt(R.drawable.ic_tiger, R.drawable.ic_goat)
        PieceSet.TOKEN -> PieceArt(R.drawable.ic_tiger_token, R.drawable.ic_goat_token)
        PieceSet.GLYPH -> PieceArt(R.drawable.ic_tiger_glyph, R.drawable.ic_goat_glyph)
        PieceSet.FESTIVAL -> PieceArt(R.drawable.ic_tiger_mask, R.drawable.ic_goat_chyangra)
        PieceSet.SNOW -> PieceArt(R.drawable.ic_tiger_snow, R.drawable.ic_goat_black)
        PieceSet.NIGHT_EYES -> PieceArt(R.drawable.ic_tiger_night_eyes, R.drawable.ic_goat_night_eyes)
    }

/** The player's piece set, provided once at the app root; previews and tests get the classic pieces. */
val LocalPieceArt = staticCompositionLocalOf { PieceSet.CLASSIC.art }
