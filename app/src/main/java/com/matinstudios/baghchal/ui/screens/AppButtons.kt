package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matinstudios.baghchal.R

// One raised family, in Home's night style: a face sitting on a lip that sinks when pressed.
// PrimaryButton (coral) for the one main action, SecondaryButton (frosted) beside it, ToolButton
// (frosted, coloured icon chip) for the game's Undo/Hint, HomeIconButton for square icons and
// QuietButton (no face, gold text) for small actions and dialogs. Dialogs pair a QuietButton
// dismiss with a small PrimaryButton confirm. Back is never a button at the bottom: it is
// BackTopBar's arrow, where the menu button sits.

/** Height of large primary and secondary buttons. */
val MainButtonHeight = 56.dp

/** Size of a raised button: large for screen actions, small for dialog confirms. */
enum class ButtonSize(
    internal val height: Dp,
    internal val corner: Dp,
    internal val lip: Dp,
    internal val padding: Dp,
    internal val fontSize: TextUnit,
) {
    Large(MainButtonHeight, 18.dp, 5.dp, 24.dp, 18.sp),
    Small(40.dp, 14.dp, 3.dp, 16.dp, 15.sp),
}

/** The one main action on a screen: raised coral, like Home's Play with AI. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Large,
) {
    MainButton(
        text, onClick, modifier, icon, enabled, size,
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
    size: ButtonSize = ButtonSize.Large,
) {
    MainButton(
        text, onClick, modifier, icon, enabled, size,
        face = HomeNight.Frost, lip = HomeNight.FrostLip, border = HomeNight.FrostBorder,
    )
}

/** A disabled button keeps its shape but fades out. */
private const val DISABLED_ALPHA = 0.45f

@Composable
private fun MainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    @DrawableRes icon: Int?,
    enabled: Boolean,
    size: ButtonSize,
    face: Color,
    lip: Color,
    border: Color?,
) {
    RaisedButton(
        onClick = onClick,
        face = face,
        lip = lip,
        shape = RoundedCornerShape(size.corner),
        lipHeight = size.lip,
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        faceModifier = Modifier.heightIn(min = size.height),
        border = border,
        enabled = enabled,
    ) {
        val start = if (icon == null) size.padding else size.padding - 4.dp
        Row(
            Modifier.padding(start = start, end = size.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) { ButtonLabel(text, icon, size.fontSize) }
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
private fun RowScope.ButtonLabel(text: String, @DrawableRes icon: Int?, fontSize: TextUnit) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = if (icon != null) Modifier.padding(start = 10.dp) else Modifier,
    )
}

/** Colour of a [ToolButton]'s icon chip. */
enum class ToolColor(internal val chip: Color, internal val icon: Color) {
    Cyan(HomeNight.Cyan, HomeNight.OnCyan),
    Gold(HomeNight.Gold, HomeNight.OnGold),
}

/**
 * The game's Undo / Hint: frosted glass, 48 dp, its icon on a coloured chip. When it plays a
 * rewarded ad an "AD" badge says so up front (AdMob requires it); screen readers hear [adLabel].
 */
@Composable
fun ToolButton(
    text: String,
    @DrawableRes icon: Int,
    color: ToolColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    adLabel: String? = null,
) {
    RaisedButton(
        onClick = onClick,
        face = HomeNight.Frost,
        lip = HomeNight.FrostLip,
        shape = RoundedCornerShape(16.dp),
        lipHeight = 4.dp,
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        faceModifier = Modifier.heightIn(min = 48.dp),
        border = HomeNight.FrostBorder,
        enabled = enabled,
    ) {
        val spoken = adLabel ?: text
        Row(
            Modifier
                .padding(start = 8.dp, end = 14.dp)
                .clearAndSetSemantics { this.text = AnnotatedString(spoken) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).background(color.chip, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = color.icon, modifier = Modifier.size(20.dp))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (adLabel != null) AdBadge()
        }
    }
}

/** Small gold pill with a play triangle: this button plays a video ad first. */
@Composable
private fun AdBadge() {
    Row(
        Modifier
            .background(HomeNight.Gold.copy(alpha = 0.2f), CircleShape)
            .padding(start = 6.dp, end = 7.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = HomeNight.Link, modifier = Modifier.size(10.dp))
        Text(
            stringResource(R.string.ad_badge),
            color = HomeNight.Link,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

/** Soft red for a quiet button that deletes something. */
private val QuietDanger = Color(0xFFFFA48F)

/**
 * A small action with no face: lamp-gold text that lights a frosted patch while pressed.
 * Used for secondary links and dialog dismiss buttons; [danger] turns it soft red.
 */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    @DrawableRes trailingIcon: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (danger) QuietDanger else HomeNight.Link
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (pressed) Color.White.copy(alpha = 0.1f) else Color.Transparent, shape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailingIcon != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        Text(text, color = color, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (trailingIcon != null) {
            Icon(painterResource(trailingIcon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
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
