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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R

// The app's buttons come in four kinds (design: "Bagh Chal button system"):
// PrimaryButton for the one main action, SecondaryButton beside it, the game's tonal Undo/Hint
// (AssistButton in GameScreen) and Material's TextButton for small actions and dialogs.
// Back is never a button at the bottom: it is BackTopBar's arrow, where the menu button sits.

/** Height of primary and secondary buttons. */
val MainButtonHeight = 56.dp

private val MainButtonPadding = PaddingValues(horizontal = 24.dp)
private val MainButtonIconPadding = PaddingValues(start = 20.dp, end = 24.dp)

/** The one main action on a screen: filled, tiger orange, 56 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MainButtonHeight),
        enabled = enabled,
        contentPadding = if (icon == null) MainButtonPadding else MainButtonIconPadding,
    ) { ButtonLabel(text, icon) }
}

/** The other choice next to a primary: outlined, same size. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MainButtonHeight),
        enabled = enabled,
        contentPadding = if (icon == null) MainButtonPadding else MainButtonIconPadding,
    ) { ButtonLabel(text, icon) }
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
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
    }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = if (icon != null) Modifier.padding(start = 10.dp) else Modifier,
    )
}

/** Top strip of a screen you leave with Back: the arrow sits exactly where the menu button does. */
@Composable
fun BackTopBar(onBack: () -> Unit, modifier: Modifier = Modifier, @StringRes title: Int? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MenuTopBarHeight).padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
        }
        if (title != null) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}
