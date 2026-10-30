package com.matinstudios.baghchal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ads.BannerAd
import com.matinstudios.baghchal.ads.BannerPlacement
import com.matinstudios.baghchal.data.SavedGame
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.board.ChaseBanner
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Each mode row's colour: Friend sky blue, Quick Match pink, Pass n Play green. */
private val FriendColors = ModeColors(Color(0xFF38BDF8), Color(0xFF0E7AB0), Color(0xFFCDEFFF))
private val OnlineColors = ModeColors(Color(0xFFFF5C8A), Color(0xFFB02A55), Color(0xFFFFD3E0))
private val PassColors = ModeColors(Color(0xFF34D399), Color(0xFF0F8A5F), Color(0xFFC9F5E4))

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
    onGoAdFree: () -> Unit,
    modifier: Modifier = Modifier,
    barPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // The side menu is a light sheet in the light theme: the bar icons follow the theme while it is open.
    WhiteSystemBarIcons(enabled = drawerState.targetValue == DrawerValue.Closed)
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    val showRemoveAds by viewModel.showRemoveAds.collectAsStateWithLifecycle()
    // Back on Home asks before closing the app (an open side menu just closes).
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else confirmExit = true
    }
    if (confirmExit) {
        YesNoPrompt(
            title = stringResource(R.string.exit_app_title),
            message = stringResource(R.string.exit_app_text),
            yes = stringResource(R.string.yes),
            no = stringResource(R.string.no),
            icon = LocalPieceArt.current.goat,
            onYes = {
                confirmExit = false
                activity?.finish()
            },
            onNo = { confirmExit = false },
        )
    }
    // Shop, Stats and Settings sit in the top-right corner; the side menu has the Shop too.
    val menuItems = listOf(
        AppMenuItem(R.string.home_learn_title, R.drawable.ic_help, detail = R.string.home_learn_detail, onClick = onHowToPlay),
        AppMenuItem(R.string.home_shop, R.drawable.ic_shop, onShop),
    )
    // The sky runs up behind the status bar and the mountain blue down behind the navigation bar;
    // the content and the banner stay clear of both.
    Column(
        modifier
            .fillMaxSize()
            .background(HomeNight.Mountain)
            .padding(bottom = barPadding.calculateBottomPadding()),
    ) {
        AppMenuDrawer(menuItems, Modifier.weight(1f), drawerState = drawerState) { openMenu ->
            HomeContent(
                savedGame = savedGame,
                onPlayVsAi = onPlayVsAi,
                onPlayOnline = onPlayOnline,
                onPlayWithFriend = onPlayWithFriend,
                onStartGame = onStartGame,
                onResume = onResume,
                onRemoveAds = onGoAdFree.takeIf { showRemoveAds },
                onShop = onShop,
                onStats = onStats,
                onSettings = onSettings,
                onOpenMenu = openMenu,
                topInset = barPadding.calculateTopPadding(),
            )
        }
        // A gap keeps the banner clear of the buttons and the falling snow.
        if (showBanner) BannerAd(BannerPlacement.HOME, Modifier.padding(top = 8.dp, bottom = 8.dp))
    }
}

/**
 * Home, "Himalaya snow night": the name on a swinging ice sign, the chase scene in a frosty frame,
 * then Play with AI and the other modes, over a snowy night sky. The rules are in the side menu.
 */
@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onRemoveAds: (() -> Unit)?,
    onShop: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
) {
    val clock = rememberSceneClock()
    Box(modifier.fillMaxSize()) {
        NightSkyBackdrop(clock, Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = topInset, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                // The sign hangs from the top edge; the corner buttons sit over its chains.
                Box(Modifier.fillMaxWidth()) {
                    IceSign(clock, Modifier.align(Alignment.TopCenter))
                    HomeTopBar(onOpenMenu, onRemoveAds, onShop, onStats, onSettings)
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
                        colors = FriendColors,
                        clock = clock,
                        index = 1,
                        onClick = onPlayWithFriend,
                    )
                    FrostModeButton(
                        title = R.string.mode_online,
                        detail = R.string.mode_online_detail,
                        icon = R.drawable.ic_mode_online_line,
                        colors = OnlineColors,
                        clock = clock,
                        index = 2,
                        onClick = onPlayOnline,
                    )
                    FrostModeButton(
                        title = R.string.mode_vs_player,
                        detail = R.string.mode_vs_player_detail,
                        icon = R.drawable.ic_mode_vs_player_line,
                        colors = PassColors,
                        clock = clock,
                        index = 3,
                        onClick = { onStartGame(GameSetup.TWO_PLAYER) },
                    )
                }
            }
        }
    }
}

/**
 * White status and navigation bar icons while Home (always a night scene) shows and [enabled];
 * otherwise, and once Home leaves, the theme's own icons.
 */
@Composable
private fun WhiteSystemBarIcons(enabled: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    val themeIconsDark = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(activity, themeIconsDark, enabled) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val darkIcons = !enabled && themeIconsDark
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
        onDispose {
            controller.isAppearanceLightStatusBars = themeIconsDark
            controller.isAppearanceLightNavigationBars = themeIconsDark
        }
    }
}

/**
 * Menu button (frosted) top-left; Remove Ads (coral, until bought), Shop (gold), Stats (cyan) and
 * Settings (violet) top-right.
 */
@Composable
private fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onRemoveAds: (() -> Unit)?,
    onShop: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeIconButton(R.drawable.ic_menu, R.string.home_menu, onOpenMenu)
        Spacer(Modifier.weight(1f))
        if (onRemoveAds != null) {
            HomeIconButton(R.drawable.ic_remove_ads, R.string.home_remove_ads, onRemoveAds, HomeIconColors.Coral)
        }
        HomeIconButton(R.drawable.ic_shop, R.string.home_shop, onShop, HomeIconColors.Gold)
        HomeIconButton(R.drawable.ic_stats, R.string.home_stats, onStats, HomeIconColors.Cyan)
        HomeIconButton(R.drawable.ic_settings, R.string.home_settings, onSettings, HomeIconColors.Violet)
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
            onRemoveAds = {},
            onShop = {},
            onStats = {},
            onSettings = {},
            onOpenMenu = {},
        )
    }
}
