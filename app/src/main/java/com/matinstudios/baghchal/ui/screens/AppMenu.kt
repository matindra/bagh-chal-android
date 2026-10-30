package com.matinstudios.baghchal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.BuildConfig
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import kotlinx.coroutines.launch

const val WEBSITE_URL = "https://matinstudios.com/bagh-chal-tiger-goat-game/"
const val PRIVACY_POLICY_URL = "https://matinstudios.com/bagh-chal-tiger-goat-game/privacy-policy"
const val MORE_GAMES_URL = "https://play.google.com/store/apps/developer?id=Matin+Studios"

/** One entry in the side menu; [detail] is an optional smaller second line. */
data class AppMenuItem(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    @StringRes val detail: Int? = null,
)

/**
 * Side menu (Home and game screens), opened by [AppMenuButton]. It covers only [content]; screens
 * keep their banner outside it so the menu never hides an ad.
 */
@Composable
fun AppMenuDrawer(
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    // Every screen's menu ends with our other games on Play, the game's web page and privacy policy (open in the browser).
    val allItems = items + listOf(
        AppMenuItem(R.string.menu_more_games, R.drawable.ic_more_games, { runCatching { uriHandler.openUri(MORE_GAMES_URL) } }),
        AppMenuItem(R.string.menu_website, R.drawable.ic_globe, { runCatching { uriHandler.openUri(WEBSITE_URL) } }),
        AppMenuItem(R.string.menu_privacy_policy, R.drawable.ic_privacy, { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } }),
    )
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            // Rows float and shine like Home's; the clock only ticks while the menu is showing.
            val clock = rememberSceneClock(running = drawerState.currentValue == DrawerValue.Open || drawerState.isAnimationRunning)
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerShape = MenuShape,
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(HomeNight.SkyTop, HomeNight.SkyMiddle, HomeNight.SkyBottom)), MenuShape)
                    .border(1.5.dp, HomeNight.FrostBorder, MenuShape),
            ) {
                MenuHeader()
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    allItems.forEachIndexed { index, item ->
                        MenuRow(item, clock, index) {
                            // Close first, so coming back to Home finds the menu shut.
                            scope.launch {
                                drawerState.close()
                                item.onClick()
                            }
                        }
                    }
                }
                BasicText(
                    stringResource(R.string.menu_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = TextStyle(color = HomeNight.OnFrostSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        },
    ) {
        content { scope.launch { drawerState.open() } }
    }
}

private val MenuShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)

/** The game's name in big glossy letters, with a tiger and a goat on frosted badges. */
@Composable
private fun MenuHeader() {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PieceBadge(LocalPieceArt.current.tiger, ChipColors.Coral)
        PieceBadge(LocalPieceArt.current.goat, ChipColors.Green)
        BasicText(
            stringResource(R.string.app_name),
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Black,
                shadow = Shadow(HomeNight.CoralLip, with(LocalDensity.current) { Offset(0f, 3.dp.toPx()) }, 0f),
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 28.sp),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** A piece on a round glass badge rimmed in its side's colour. */
@Composable
private fun PieceBadge(@DrawableRes art: Int, colors: ModeColors) {
    Box(
        Modifier
            .size(44.dp)
            .background(colors.chip.copy(alpha = 0.25f), CircleShape)
            .border(1.5.dp, colors.chip, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(art), contentDescription = null, modifier = Modifier.size(34.dp))
    }
}

/** Each menu entry's colour comes from its icon, so an entry looks the same on every screen. */
private fun menuColors(@DrawableRes icon: Int): ModeColors = when (icon) {
    R.drawable.ic_home -> ChipColors.Coral
    R.drawable.ic_help -> ChipColors.Green
    R.drawable.ic_stats -> ChipColors.Orange
    R.drawable.ic_shop -> ChipColors.Gold
    R.drawable.ic_settings -> ChipColors.Violet
    R.drawable.ic_more_games -> ChipColors.Pink
    R.drawable.ic_globe -> ChipColors.Sky
    else -> ChipColors.Teal
}

/** A menu entry as a raised tinted-glass row with a coloured icon chip and a chevron. */
@Composable
private fun MenuRow(item: AppMenuItem, clock: State<Long>, index: Int, onClick: () -> Unit) {
    val colors = menuColors(item.icon)
    RaisedButton(
        onClick = onClick,
        face = colors.chip.copy(alpha = 0.16f),
        lip = HomeNight.FrostLip,
        shape = RoundedCornerShape(16.dp),
        lipHeight = 4.dp,
        modifier = Modifier.fillMaxWidth().iceFloat(clock, index),
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        border = colors.chip.copy(alpha = 0.5f),
        decoration = { drawShineWave(clock, index) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconChip(item.icon, colors.chip, colors.chipLip, Color.White, size = 36)
            Column(Modifier.weight(1f)) {
                BasicText(
                    stringResource(item.label),
                    style = TextStyle(color = HomeNight.OnFrost, fontWeight = FontWeight.Black, fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.detail?.let {
                    BasicText(
                        stringResource(it),
                        style = TextStyle(color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = HomeNight.OnFrostSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The top strip of every screen with the side menu: the menu button always sits in the same
 * top-left spot, with [content] (title, status) beside it and an optional [trailing] button
 * mirrored in the top-right corner.
 */
@Composable
fun MenuTopBar(
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit = {},
) {
    // The bar grows for tall content, but the buttons stay pinned to the top corners.
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MenuTopBarHeight)
                .padding(start = MenuButtonSpace, end = if (trailing != null) MenuButtonSpace else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        AppMenuButton(onOpenMenu, Modifier.padding(start = TopBarButtonStart, top = 2.dp))
        if (trailing != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(end = TopBarButtonStart, top = 2.dp)) { trailing() }
        }
    }
}

/** The game screens' top-right button: silences music and sound effects during games. */
@Composable
fun MuteButton(muted: Boolean, onToggle: () -> Unit) {
    HomeIconButton(
        icon = if (muted) R.drawable.ic_sound_off else R.drawable.ic_sound_on,
        label = if (muted) R.string.game_unmute else R.string.game_mute,
        onClick = onToggle,
    )
}

/** Space before the frosted menu / back button, from the screen's left edge. */
val TopBarButtonStart = 16.dp

/** Width the menu button takes from the left edge (margin, 48 dp button, gap). */
val MenuButtonSpace = 76.dp

val MenuTopBarHeight = 56.dp

/** Frosted menu button, the same as Home's. */
@Composable
fun AppMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) { HomeIconButton(R.drawable.ic_menu, R.string.home_menu, onClick) }
}
