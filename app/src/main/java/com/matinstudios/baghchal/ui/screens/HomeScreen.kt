package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextAlign
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
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun HomeScreen(
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val showBanner by viewModel.showBanner.collectAsStateWithLifecycle()
    val menuItems = listOf(
        AppMenuItem(R.string.home_how_to_play, R.drawable.ic_help, onHowToPlay),
        AppMenuItem(R.string.home_stats, R.drawable.ic_stats, onStats),
        AppMenuItem(R.string.home_settings, R.drawable.ic_settings, onSettings),
    )
    Column(modifier.fillMaxSize()) {
        AppMenuDrawer(menuItems, Modifier.weight(1f)) { openMenu ->
            HomeContent(savedGame, onPlayVsAi, onPlayOnline, onPlayWithFriend, onStartGame, onResume, openMenu)
        }
        if (showBanner) BannerAd(BannerPlacement.HOME, Modifier.padding(top = 8.dp))
    }
}

/** Home: resume a saved game, or pick a mode, grouped into online and on-this-phone. */
@Composable
fun HomeContent(
    savedGame: SavedGame?,
    onPlayVsAi: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayWithFriend: () -> Unit,
    onStartGame: (GameSetup) -> Unit,
    onResume: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HomeHeader()
                if (savedGame != null) ResumeCard(savedGame, onResume)
                ChaseBanner(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clearAndSetSemantics {},
                )

                ModeSection(R.string.home_section_online, R.drawable.ic_globe) {
                    ModeTile(
                        title = R.string.mode_online,
                        detail = R.string.mode_online_detail,
                        icon = R.drawable.ic_mode_online,
                        container = colors.tertiaryContainer,
                        content = colors.onTertiaryContainer,
                        onClick = onPlayOnline,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    ModeTile(
                        title = R.string.mode_friend,
                        detail = R.string.mode_friend_detail,
                        icon = R.drawable.ic_mode_friend,
                        container = colors.tertiaryContainer,
                        content = colors.onTertiaryContainer,
                        onClick = onPlayWithFriend,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                ModeSection(R.string.home_section_device, R.drawable.ic_phone) {
                    ModeTile(
                        title = R.string.mode_vs_ai,
                        detail = R.string.mode_vs_ai_detail,
                        icon = R.drawable.ic_mode_vs_ai,
                        container = colors.primaryContainer,
                        content = colors.onPrimaryContainer,
                        onClick = onPlayVsAi,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    ModeTile(
                        title = R.string.mode_vs_player,
                        detail = R.string.mode_vs_player_detail,
                        icon = R.drawable.ic_mode_vs_player,
                        container = colors.secondaryContainer,
                        content = colors.onSecondaryContainer,
                        onClick = { onStartGame(GameSetup.TWO_PLAYER) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        // Drawn after the content so it sits on top of the scrolling column.
        MenuTopBar(onOpenMenu, Modifier.align(Alignment.TopStart))
    }
}

/** Tiger, name, goat on one line, the tagline below. */
@Composable
private fun HomeHeader() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.ic_tiger), contentDescription = null, modifier = Modifier.size(48.dp))
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
            Image(painterResource(R.drawable.ic_goat), contentDescription = null, modifier = Modifier.size(48.dp))
        }
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A small caps heading over two mode tiles of equal height. */
@Composable
private fun ModeSection(@StringRes title: Int, @DrawableRes icon: Int, tiles: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(title).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp), content = tiles)
    }
}

@Composable
private fun ModeTile(
    @StringRes title: Int,
    @StringRes detail: Int,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(60.dp))
            }
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(detail), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResumeCard(savedGame: SavedGame, onResume: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_resume_title), style = MaterialTheme.typography.titleLarge)
            val aiSide = savedGame.aiSide
            val mode = if (aiSide == null) {
                stringResource(R.string.home_two_player_short)
            } else {
                val side = stringResource(if (aiSide.opponent == Side.GOAT) R.string.side_goats else R.string.side_tigers)
                stringResource(R.string.home_resume_vs_computer, side, stringResource(savedGame.difficulty.label))
            }
            Text(mode, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.home_resume_progress,
                    savedGame.game.goatsInHand,
                    savedGame.game.goatsCaptured,
                    GOATS_TO_WIN,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(stringResource(R.string.home_resume), onResume, Modifier.fillMaxWidth())
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
    BaghchalTheme { HomeContent(savedGame = null, onPlayVsAi = {}, onPlayOnline = {}, onPlayWithFriend = {}, onStartGame = {}, onResume = {}, onOpenMenu = {}) }
}
