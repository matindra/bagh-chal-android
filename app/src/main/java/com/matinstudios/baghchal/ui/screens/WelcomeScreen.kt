package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.tutorial.Lesson
import com.matinstudios.baghchal.tutorial.Nudge
import com.matinstudios.baghchal.tutorial.Stage
import com.matinstudios.baghchal.tutorial.Tutorial
import com.matinstudios.baghchal.tutorial.TutorialScript
import com.matinstudios.baghchal.tutorial.TutorialState
import com.matinstudios.baghchal.ui.board.BoardCanvas
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** First launch (and How to Play): a guided game on a real board. The player is the goats. */
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(factory = WelcomeViewModel.Factory),
) {
    val theme = viewModel.boardTheme.collectAsStateWithLifecycle().value ?: return
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finish = {
        viewModel.onFinished()
        onFinished()
    }
    WelcomeContent(
        state = state,
        boardTheme = theme,
        onPointTapped = viewModel::onPointTapped,
        onNext = { if (!viewModel.onNext()) finish() },
        onSkip = finish,
        modifier = modifier,
    )
}

@Composable
fun WelcomeContent(
    state: TutorialState,
    boardTheme: BoardTheme,
    onPointTapped: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished = state.isLastStep && state.stage == Stage.DONE
    // The night sky behind it comes from the navigation graph, like every other screen.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepChip(state.stepIndex + 1, TutorialScript.STEPS.size)
            Spacer(Modifier.weight(1f))
            // Hidden (but still taking space) at the end, where the main button already starts playing.
            QuietButton(
                stringResource(R.string.welcome_skip),
                onClick = onSkip,
                enabled = !finished,
                modifier = Modifier.alpha(if (finished) 0f else 1f),
            )
        }

        BoardCanvas(
            board = state.game.board,
            theme = boardTheme,
            selected = state.selected,
            targets = state.targets,
            trappedTigers = state.trappedTigers,
            lastMove = state.lastMove,
            moveNumber = state.moveNumber,
            hint = state.arrow,
            onPointTapped = onPointTapped,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .weight(1f, fill = false)
                .aspectRatio(1f),
        )

        CoachCard(state, Modifier.widthIn(max = 600.dp).fillMaxWidth())

        // Shown only when the step is done; kept in the layout so the board doesn't jump.
        val showButton = state.stage == Stage.DONE
        PrimaryButton(
            text = stringResource(if (finished) R.string.welcome_start else R.string.welcome_next),
            onClick = onNext,
            enabled = showButton,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .alpha(if (showButton) 1f else 0f),
        )
    }
}

@Composable
private fun StepChip(number: Int, count: Int) {
    Text(
        stringResource(R.string.tutorial_step_count, number, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Frosted card under the board: the lesson, what to do (or what just happened), and any nudge. */
@Composable
private fun CoachCard(state: TutorialState, modifier: Modifier = Modifier) {
    val lesson = state.step.lesson
    val text = lesson.text
    val tigerSpeaks = state.stage == Stage.TIGER_TURN || (state.stage == Stage.DONE && state.step.reply != null)
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .heightIn(min = 148.dp)
            .clip(shape)
            .background(CardFace, shape)
            .border(1.dp, CardRim, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val art = LocalPieceArt.current
        Image(
            painterResource(if (tigerSpeaks) art.tiger else art.goat),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(text.title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (lesson == Lesson.MEET) {
                Text(
                    stringResource(R.string.welcome_tagline),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            val body = when (state.stage) {
                Stage.INSTRUCTION -> text.instruction
                Stage.TIGER_TURN -> R.string.tutorial_tiger_turn
                Stage.DONE -> text.done ?: text.instruction
            }
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            state.nudge?.let { nudge ->
                Text(
                    stringResource(nudge.text),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (nudge == Nudge.TIGER_WOULD_EAT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
        }
    }
}

private val CardFace = Color.White.copy(alpha = 0.10f)
private val CardRim = Color.White.copy(alpha = 0.35f)

private class LessonText(@StringRes val title: Int, @StringRes val instruction: Int, @StringRes val done: Int?)

private val Lesson.text: LessonText
    get() = when (this) {
        Lesson.MEET -> LessonText(R.string.welcome_title, R.string.tutorial_meet_body, null)
        Lesson.FIRST_GOAT -> LessonText(
            R.string.tutorial_first_goat_title,
            R.string.tutorial_first_goat_instruction,
            R.string.tutorial_first_goat_done,
        )
        Lesson.BLOCK -> LessonText(R.string.tutorial_block_title, R.string.tutorial_block_instruction, R.string.tutorial_block_done)
        Lesson.CARELESS -> LessonText(
            R.string.tutorial_careless_title,
            R.string.tutorial_careless_instruction,
            R.string.tutorial_careless_done,
        )
        Lesson.SAFE_SPOT -> LessonText(R.string.tutorial_safe_title, R.string.tutorial_safe_instruction, R.string.tutorial_safe_done)
        Lesson.TRAP -> LessonText(R.string.tutorial_trap_title, R.string.tutorial_trap_instruction, R.string.tutorial_trap_done)
    }

@get:StringRes
private val Nudge.text: Int
    get() = when (this) {
        Nudge.TAP_GLOWING -> R.string.tutorial_nudge_glowing
        Nudge.TIGER_WOULD_EAT -> R.string.tutorial_nudge_danger
        Nudge.TAP_GOAT -> R.string.tutorial_nudge_goat
    }

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    BaghchalTheme {
        WelcomeContent(
            state = Tutorial.next(Tutorial.start())!!,
            boardTheme = BoardTheme.WOOD,
            onPointTapped = {},
            onNext = {},
            onSkip = {},
        )
    }
}
