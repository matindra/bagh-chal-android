package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R

// The app's buttons come in four kinds, all in Home's raised night style:
// PrimaryButton (coral) for the one main action, SecondaryButton (frosted) beside it, the game's
// tonal Undo/Hint (AssistButton in GameScreen) and Material's TextButton for small actions and dialogs.
// Back is never a button at the bottom: it is BackTopBar's arrow, where the menu button sits.

/** Height of primary and secondary buttons. */
val MainButtonHeight = 56.dp

private val MainButtonPadding = PaddingValues(horizontal = 24.dp)
private val MainButtonIconPadding = PaddingValues(start = 20.dp, end = 24.dp)

/** The one main action on a screen: raised coral, like Home's Play with AI, 56 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    MainButton(
        text, onClick, modifier, icon, enabled,
        face = HomeNight.Coral, lip = HomeNight.CoralLip, border = null,
    )
}

/** The other choice next to a primary: raised frosted glass, same size. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    MainButton(
        text, onClick, modifier, icon, enabled,
        face = HomeNight.Frost, lip = HomeNight.FrostLip, border = HomeNight.FrostBorder,
    )
}

/** A disabled main button keeps its shape but fades out. */
private const val DISABLED_ALPHA = 0.45f

@Composable
private fun MainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    @DrawableRes icon: Int?,
    enabled: Boolean,
    face: Color,
    lip: Color,
    border: Color?,
) {
    RaisedButton(
        onClick = onClick,
        face = face,
        lip = lip,
        shape = RoundedCornerShape(18.dp),
        lipHeight = 5.dp,
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        faceModifier = Modifier.heightIn(min = MainButtonHeight),
        border = border,
        enabled = enabled,
    ) {
        Row(
            Modifier.padding(if (icon == null) MainButtonPadding else MainButtonIconPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) { ButtonLabel(text, icon) }
    }
}

/** Secondary on the left, primary on the right, equal widths. */
@Composable
fun ButtonPair(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton(secondary, onSecondary, Modifier.weight(1f))
        PrimaryButton(primary, onPrimary, Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.ButtonLabel(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = if (icon != null) Modifier.padding(start = 10.dp) else Modifier,
    )
}

/**
 * Top strip of a screen you leave with Back: a frosted arrow button (like Home's corner buttons)
 * exactly where the menu button sits.
 */
@Composable
fun BackTopBar(onBack: () -> Unit, modifier: Modifier = Modifier, @StringRes title: Int? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MenuTopBarHeight).padding(start = TopBarButtonStart, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeIconButton(R.drawable.ic_back, R.string.back, onBack)
        if (title != null) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}
