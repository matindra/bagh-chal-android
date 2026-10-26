package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalThemeExtras
import com.matinstudios.baghchal.ui.theme.PrayerFlags
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Player vs AI: pick a side and a difficulty, then start. */
@Composable
fun AiSetupScreen(onStart: (GameSetup) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var humanSide by rememberSaveable { mutableStateOf(Side.GOAT) }
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }

    Column(modifier.fillMaxSize()) {
        BackTopBar(onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            PrayerFlags()
            Text(
                stringResource(R.string.mode_vs_ai),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )

            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel(R.string.home_play_as)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SideOption(
                        side = Side.GOAT,
                        selected = humanSide == Side.GOAT,
                        onClick = { humanSide = Side.GOAT },
                        detail = stringResource(R.string.side_goats_detail),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    SideOption(
                        side = Side.TIGER,
                        selected = humanSide == Side.TIGER,
                        onClick = { humanSide = Side.TIGER },
                        detail = stringResource(R.string.side_tigers_detail, GOATS_TO_WIN),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }

                SectionLabel(R.string.home_difficulty)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (level in Difficulty.entries) {
                        DifficultyOption(level, selected = difficulty == level, onClick = { difficulty = level })
                    }
                }

                PrimaryButton(
                    text = stringResource(R.string.ai_setup_start),
                    onClick = { onStart(GameSetup.vsComputer(humanSide, difficulty)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun SectionLabel(@StringRes text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

/** Outlined card; when selected it takes a thicker [accent] border and a [selectedContainer] fill. */
@Composable
private fun OptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    selectedContainer: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val border = if (selected) BorderStroke(2.dp, accent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val container by animateColorAsState(
        if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "option container",
    )
    OutlinedCard(
        border = border,
        colors = CardDefaults.outlinedCardColors(containerColor = container),
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) { content() }
}

@Composable
internal fun SideOption(
    side: Side,
    selected: Boolean,
    onClick: () -> Unit,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isTiger = side == Side.TIGER
    OptionCard(
        selected = selected,
        onClick = onClick,
        accent = if (isTiger) colors.primary else colors.secondary,
        selectedContainer = if (isTiger) colors.primaryContainer else colors.secondaryContainer,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painterResource(if (isTiger) LocalPieceArt.current.tiger else LocalPieceArt.current.goat),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
            Text(
                stringResource(if (isTiger) R.string.side_tigers else R.string.side_goats),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DifficultyOption(level: Difficulty, selected: Boolean, onClick: () -> Unit) {
    val accent = level.accent
    OptionCard(
        selected = selected,
        onClick = onClick,
        accent = accent,
        selectedContainer = accent.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // The whole card is the radio button, so this one only shows the state.
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = accent),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(level.label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(level.detail), style = MaterialTheme.typography.bodySmall)
            }
            StrengthBars(filled = level.ordinal + 1, total = Difficulty.entries.size, color = accent)
        }
    }
}

/** Rising bars, like a signal meter: one for Easy, all for Hard. */
@Composable
private fun StrengthBars(filled: Int, total: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..total) {
            Box(
                Modifier
                    .width(6.dp)
                    .height((8 + i * 5).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= filled) color else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

private val Difficulty.accent: Color
    @Composable get() = when (this) {
        Difficulty.EASY -> BaghchalThemeExtras.colors.easy
        Difficulty.MEDIUM -> BaghchalThemeExtras.colors.medium
        Difficulty.HARD -> BaghchalThemeExtras.colors.hard
    }

private val Difficulty.detail: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy_detail
        Difficulty.MEDIUM -> R.string.difficulty_medium_detail
        Difficulty.HARD -> R.string.difficulty_hard_detail
    }

@Preview(showBackground = true)
@Composable
private fun AiSetupPreview() {
    BaghchalTheme { AiSetupScreen(onStart = {}, onBack = {}) }
}
