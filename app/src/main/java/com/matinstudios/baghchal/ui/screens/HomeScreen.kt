package com.matinstudios.baghchal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.board.ChaseBanner
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Mode chip colours (each with its darker lip). */
private val FriendChip = Color(0xFFFF5C8A) to Color(0xFFB02A55)
private val OnlineChip = Color(0xFF38BDF8) to Color(0xFF0E7AB0)
private val PassChip = Color(0xFF34D399) to Color(0xFF0F8A5F)

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
    // Shop, Stats and Settings sit in the top-right corner; the side menu keeps the rest.
    val menuItems = listOf(AppMenuItem(R.string.home_how_to_play, R.drawable.ic_help, onHowToPlay))
    Column(modifier.fillMaxSize().background(HomeNight.Mountain)) {
        AppMenuDrawer(menuItems, Modifier.weight(1f)) { openMenu ->
            HomeContent(
                savedGame = savedGame,
                onPlayVsAi = onPlayVsAi,
                onPlayOnline = onPlayOnline,
                onPlayWithFriend = onPlayWithFriend,
                onStartGame = onStartGame,
                onResume = onResume,
                onHowToPlay = onHowToPlay,
                onShop = onShop,
                onStats = onStats,
                onSettings = onSettings,
                onOpenMenu = openMenu,
            )
        }
        // A gap keeps the banner clear of the buttons and the falling snow.
        if (showBanner) BannerAd(BannerPlacement.HOME, Modifier.padding(top = 8.dp, bottom = 8.dp))
    }
}

/**
 * Home, "Himalaya snow night": the name on a swinging ice sign, the chase scene in a frosty frame,
 * then Play with AI, the other modes and a link to the rules, over a snowy night sky.
 */
@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onHowToPlay: () -> Unit,
    onShop: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = rememberSceneClock()
    Box(modifier.fillMaxSize()) {
        NightSkyBackdrop(clock, Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                // The sign hangs from the top edge; the corner buttons sit over its chains.
                Box(Modifier.fillMaxWidth()) {
                    IceSign(clock, Modifier.align(Alignment.TopCenter))
                    HomeTopBar(onOpenMenu, onShop, onStats, onSettings)
                }
                Spacer(Modifier.height(14.dp))
                FrostFrame(Modifier.fillMaxWidth()) {
                    ChaseBanner(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(350f / 200f)
                            .clearAndSetSemantics {},
                        scenery = HomeNight.ChaseScenery,
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (savedGame != null) {
                    ResumeCard(savedGame, onResume)
                    Spacer(Modifier.height(16.dp))
                }
                PlayAiButton(clock, onPlayVsAi, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrostModeButton(
                        title = R.string.mode_friend,
                        detail = R.string.mode_friend_detail,
                        icon = R.drawable.ic_mode_friend_line,
                        chip = FriendChip.first,
                        chipLip = FriendChip.second,
                        onClick = onPlayWithFriend,
                    )
                    FrostModeButton(
                        title = R.string.mode_online,
                        detail = R.string.mode_online_detail,
                        icon = R.drawable.ic_mode_online_line,
                        chip = OnlineChip.first,
                        chipLip = OnlineChip.second,
                        onClick = onPlayOnline,
                    )
                    FrostModeButton(
                        title = R.string.mode_vs_player,
                        detail = R.string.mode_vs_player_detail,
                        icon = R.drawable.ic_mode_vs_player_line,
                        chip = PassChip.first,
                        chipLip = PassChip.second,
                        onClick = { onStartGame(GameSetup.TWO_PLAYER) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                LearnLink(onHowToPlay, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

/** Menu button top-left; Shop (gold), Stats and Settings top-right. */
@Composable
private fun HomeTopBar(onOpenMenu: () -> Unit, onShop: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeIconButton(R.drawable.ic_menu, R.string.home_menu, onOpenMenu)
        Spacer(Modifier.weight(1f))
        HomeIconButton(R.drawable.ic_shop, R.string.home_shop, onShop, gold = true)
        HomeIconButton(R.drawable.ic_stats, R.string.home_stats, onStats)
        HomeIconButton(R.drawable.ic_settings, R.string.home_settings, onSettings)
    }
}

/** "New to Bagh Chal? Learn the rules in 2 minutes" as one underlined link to How to Play. */
@Composable
private fun LearnLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.home_learn_link, stringResource(R.string.home_learn_title), stringResource(R.string.home_learn_detail)),
            color = HomeNight.Link,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center,
        )
    }
}

/** Compact frosted "game in progress" row: a little board, what the game is, and a gold Resume. */
@Composable
private fun ResumeCard(savedGame: SavedGame, onResume: () -> Unit) {
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
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .drawBehind { drawRoundRect(HomeNight.FrostLip, Offset(0f, 5.dp.toPx()), size, CornerRadius(20.dp.toPx())) }
            .background(HomeNight.Frost, shape)
            .border(BorderStroke(1.5.dp, HomeNight.FrostBorder), shape)
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniBoard(Modifier.size(40.dp).clearAndSetSemantics {})
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.home_resume_title),
                color = HomeNight.Link,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                stringResource(R.string.home_resume_summary, mode, progress),
                color = HomeNight.OnFrostSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GoldPillButton(stringResource(R.string.home_resume), onResume)
    }
}

/** A tiny board with two tigers and two goats. */
@Composable
private fun MiniBoard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val u = size.minDimension / 40f
        drawRoundRect(HomeNight.FrameFace, cornerRadius = CornerRadius(8 * u))
        val lines = HomeNight.IceInk.copy(alpha = 0.45f)
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

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun HomeScreenPreview() {
    BaghchalTheme {
        HomeContent(
            savedGame = null,
            onPlayVsAi = {},
            onPlayOnline = {},
            onPlayWithFriend = {},
            onStartGame = {},
            onResume = {},
            onHowToPlay = {},
            onShop = {},
            onStats = {},
            onSettings = {},
            onOpenMenu = {},
        )
    }
}
