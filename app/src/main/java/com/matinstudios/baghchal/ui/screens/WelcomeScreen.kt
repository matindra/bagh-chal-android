package com.matinstudios.baghchal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import com.matinstudios.baghchal.ui.theme.MountainBackdrop
import com.matinstudios.baghchal.ui.theme.PrayerFlags
import kotlinx.coroutines.launch

/** An intro page followed by one page per rule. */
val WELCOME_PAGE_COUNT = 1 + HOW_TO_PLAY_STEPS.size

/** First-launch tutorial: swipe through the rules, or skip straight to playing. */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(factory = WelcomeViewModel.Factory),
) {
    val theme = viewModel.boardTheme.collectAsStateWithLifecycle().value ?: return
    WelcomeContent(
        boardTheme = theme,
        onFinished = {
            viewModel.onFinished()
            onFinished()
        },
        modifier = modifier,
    )
}

@Composable
fun WelcomeContent(boardTheme: BoardTheme, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState { WELCOME_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == WELCOME_PAGE_COUNT - 1

    MountainBackdrop(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                // Hidden (but still taking space) on the last page, where the main button already starts the game.
                TextButton(
                    onClick = onFinished,
                    enabled = !isLastPage,
                    modifier = Modifier.alpha(if (isLastPage) 0f else 1f),
                ) { Text(stringResource(R.string.welcome_skip)) }
            }

            HorizontalPager(pagerState, Modifier.weight(1f).fillMaxWidth()) { page ->
                if (page == 0) {
                    IntroPage()
                } else {
                    RulePage(stepNumber = page, step = HOW_TO_PLAY_STEPS[page - 1], boardTheme = boardTheme)
                }
            }

            PageDots(current = pagerState.currentPage, count = WELCOME_PAGE_COUNT, Modifier.padding(vertical = 16.dp))

            PrimaryButton(
                text = stringResource(if (isLastPage) R.string.welcome_start else R.string.welcome_next),
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IntroPage() {
    WelcomePage {
        PrayerFlags()
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painterResource(LocalPieceArt.current.tiger), contentDescription = null, modifier = Modifier.size(96.dp))
            Image(painterResource(LocalPieceArt.current.goat), contentDescription = null, modifier = Modifier.size(96.dp))
        }
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.welcome_tagline),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.rules_goal), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RulePage(stepNumber: Int, step: HowToPlayStep, boardTheme: BoardTheme) {
    WelcomePage {
        Text(
            stringResource(R.string.welcome_step_count, stepNumber, HOW_TO_PLAY_STEPS.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Text(
            stringResource(step.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        RulePicture(step, boardTheme, Modifier.widthIn(max = 360.dp).fillMaxWidth())
        Text(stringResource(step.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

/** A centred page that scrolls on short screens. */
@Composable
private fun WelcomePage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
private fun PageDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.welcome_page_description, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot width")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                label = "dot color",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    BaghchalTheme { WelcomeContent(BoardTheme.WOOD, onFinished = {}) }
}
