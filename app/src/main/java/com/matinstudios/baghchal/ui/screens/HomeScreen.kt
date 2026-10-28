package com.matinstudios.baghchal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.ui.board.ChaseBanner
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import com.matinstudios.baghchal.ui.theme.BaghchalThemeExtras

@Composable
fun HomeScreen(
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    // Shop, Stats and Settings live in the bottom bar; the side menu keeps the rest.
    val menuItems = listOf(AppMenuItem(R.string.home_how_to_play, R.drawable.ic_help, onHowToPlay))
    Column(modifier.fillMaxSize()) {
        AppMenuDrawer(menuItems, Modifier.weight(1f)) { openMenu ->
            HomeContent(savedGame, onPlayVsAi, onPlayOnline, onPlayWithFriend, onStartGame, onResume, onHowToPlay, openMenu)
        }
        // A gap keeps the banner from touching the bar's buttons.
        if (showBanner) BannerAd(BannerPlacement.HOME, Modifier.padding(top = 8.dp, bottom = 8.dp))
        HomeNavBar(onShop = onShop, onStats = onStats, onSettings = onSettings)
    }
}

/** Bottom bar: Home (this screen) plus the places that used to sit in the side menu and top bar. */
@Composable
private fun HomeNavBar(onShop: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    NavigationBar {
        HomeNavItem(R.string.menu_home, R.drawable.ic_home, selected = true, onClick = {})
        HomeNavItem(R.string.home_shop, R.drawable.ic_shop, selected = false, onClick = onShop)
        HomeNavItem(R.string.home_stats, R.drawable.ic_stats, selected = false, onClick = onStats)
        HomeNavItem(R.string.home_settings, R.drawable.ic_settings, selected = false, onClick = onSettings)
    }
}

@Composable
private fun RowScope.HomeNavItem(@StringRes label: Int, @DrawableRes icon: Int, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(painterResource(icon), contentDescription = null) },
        label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

/** Home: resume a saved game, pick a mode, or learn the rules. */
@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onHowToPlay: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val extras = BaghchalThemeExtras.colors
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Room at the top for the menu button.
                .padding(start = 20.dp, end = 20.dp, top = MenuTopBarHeight, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HomeHeader()
                ChaseBanner(
                    Modifier
                        .fillMaxWidth()
                        .height(138.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clearAndSetSemantics {},
                )
                if (savedGame != null) ResumeCard(savedGame, onResume)

                // Four modes in a 2×2 grid, each its own colour; a friend game leads.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeRow {
                        ModeTile(
                            title = R.string.mode_friend,
                            detail = R.string.mode_friend_detail,
                            icon = R.drawable.ic_mode_friend_line,
                            container = extras.friendContainer,
                            content = extras.onFriendContainer,
                            accent = extras.friend,
                            onAccent = extras.onFriend,
                            phase = 0,
                            onClick = onPlayWithFriend,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ModeTile(
                            title = R.string.mode_online,
                            detail = R.string.mode_online_detail,
                            icon = R.drawable.ic_mode_online_line,
                            container = colors.tertiaryContainer,
                            content = colors.onTertiaryContainer,
                            accent = colors.tertiary,
                            onAccent = colors.onTertiary,
                            phase = 1,
                            onClick = onPlayOnline,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    ModeRow {
                        ModeTile(
                            title = R.string.mode_vs_ai,
                            detail = R.string.mode_vs_ai_detail,
                            icon = R.drawable.ic_mode_vs_ai_line,
                            container = colors.primaryContainer,
                            content = colors.onPrimaryContainer,
                            accent = colors.primary,
                            onAccent = colors.onPrimary,
                            phase = 2,
                            onClick = onPlayVsAi,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ModeTile(
                            title = R.string.mode_vs_player,
                            detail = R.string.mode_vs_player_detail,
                            icon = R.drawable.ic_mode_vs_player_line,
                            container = colors.secondaryContainer,
                            content = colors.onSecondaryContainer,
                            accent = colors.secondary,
                            onAccent = colors.onSecondary,
                            phase = 3,
                            onClick = { onStartGame(GameSetup.TWO_PLAYER) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                LearnCard(onHowToPlay)
            }
        }
        // Drawn after the content so it sits on top of the scrolling column.
        MenuTopBar(onOpenMenu, Modifier.align(Alignment.TopStart))
    }
}

/** A dashed "new here?" row under the modes that opens How to Play. */
@Composable
private fun LearnCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .dashedBorder(colors.outline, 16.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_help), contentDescription = null, tint = colors.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_learn_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.home_learn_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val inset = stroke / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx() - inset),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/** Tiger, name, goat on one line, the tagline below. */
@Composable
private fun HomeHeader() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(LocalPieceArt.current.tiger), contentDescription = null, modifier = Modifier.size(48.dp))
            // One line on every screen: shrinks from headlineLarge until it fits.
            BasicText(
                stringResource(R.string.home_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 16.sp,
                    maxFontSize = MaterialTheme.typography.headlineLarge.fontSize,
                ),
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            )
            Image(painterResource(LocalPieceArt.current.goat), contentDescription = null, modifier = Modifier.size(48.dp))
        }
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Two mode tiles side by side, of equal height. */
@Composable
private fun ModeRow(tiles: @Composable RowScope.() -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp), content = tiles)
}

/** Compact "game in progress" row: a little board, what the game is, and Resume. */
@Composable
private fun ResumeCard(savedGame: SavedGame, onResume: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val aiSide = savedGame.aiSide
    val mode = if (aiSide == null) {
        stringResource(R.string.home_two_player_short)
    } else {
        val side = stringResource(if (aiSide.opponent == Side.GOAT) R.string.side_goats else R.string.side_tigers)
        stringResource(R.string.home_resume_vs_computer, side, stringResource(savedGame.difficulty.label))
    }
    val progress = stringResource(
        R.string.home_resume_progress,
        savedGame.game.goatsInHand,
        savedGame.game.goatsCaptured,
        GOATS_TO_WIN,
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiniBoard(Modifier.size(40.dp).clearAndSetSemantics {})
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_resume_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.home_resume_summary, mode, progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onResume,
                modifier = Modifier.heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Text(stringResource(R.string.home_resume), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** A tiny board with two tigers and two goats, in the board-tinted colours. */
@Composable
private fun MiniBoard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val u = size.minDimension / 40f
        drawRoundRect(colors.primaryContainer, cornerRadius = CornerRadius(8 * u))
        val lines = colors.onPrimaryContainer.copy(alpha = 0.45f)
        val stroke = 1.2f * u
        for (k in 0..4) {
            val a = (8 + 6 * k) * u
            drawLine(lines, Offset(8 * u, a), Offset(32 * u, a), stroke)
            drawLine(lines, Offset(a, 8 * u), Offset(a, 32 * u), stroke)
        }
        drawLine(lines, Offset(8 * u, 8 * u), Offset(32 * u, 32 * u), stroke)
        drawLine(lines, Offset(32 * u, 8 * u), Offset(8 * u, 32 * u), stroke)
        drawCircle(colors.primary, 3.6f * u, Offset(8 * u, 8 * u))
        drawCircle(colors.primary, 3.6f * u, Offset(32 * u, 32 * u))
        for (goat in listOf(Offset(20 * u, 20 * u), Offset(14 * u, 26 * u))) {
            drawCircle(colors.secondaryContainer, 3.2f * u, goat)
            drawCircle(colors.secondary, 3.2f * u, goat, style = Stroke(1.4f * u))
        }
    }
}

internal val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BaghchalTheme { HomeContent(savedGame = null, onPlayVsAi = {}, onPlayOnline = {}, onPlayWithFriend = {}, onStartGame = {}, onResume = {}, onHowToPlay = {}, onOpenMenu = {}) }
}
