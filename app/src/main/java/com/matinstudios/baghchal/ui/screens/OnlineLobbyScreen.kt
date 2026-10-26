package com.matinstudios.baghchal.ui.screens

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.RoomCode
import com.matinstudios.baghchal.ui.board.LocalPieceArt
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which way in from Home: a random opponent, or a friend with a 4-digit code. */
enum class OnlineLobbyMode(@StringRes val title: Int) {
    RANDOM(R.string.mode_online),
    FRIEND(R.string.mode_friend),
}

/** Play online: a quick match against anyone, or a private game with a 4-digit code. */
@Composable
fun OnlineLobbyScreen(
    mode: OnlineLobbyMode,
    onMatched: (roomId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineLobbyViewModel = viewModel(factory = OnlineLobbyViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentOnMatched by rememberUpdatedState(onMatched)
    LaunchedEffect(viewModel) { viewModel.matched.collect { currentOnMatched(it) } }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(resources.getString(it.text)) }
    }
    Column(modifier.fillMaxSize()) {
        OnlineLobbyContent(
            mode = mode,
            isAvailable = viewModel.isAvailable,
            state = state,
            onQuickMatch = viewModel::onQuickMatch,
            onCreateGame = viewModel::onCreateGame,
            onJoin = viewModel::onJoin,
            onCancel = viewModel::onCancel,
            onShareCode = { code -> context.startActivity(shareCodeIntent(resources.getString(R.string.online_share_text, code))) },
            onBack = onBack,
            modifier = Modifier.weight(1f),
        )
        SnackbarHost(snackbar)
    }
}

@Composable
fun OnlineLobbyContent(
    mode: OnlineLobbyMode,
    isAvailable: Boolean,
    state: LobbyState,
    onQuickMatch: () -> Unit,
    onCreateGame: (Side) -> Unit,
    onJoin: (String) -> Unit,
    onCancel: () -> Unit,
    onShareCode: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // While a game is being set up, Back would strand it, so the top button cancels instead.
        BackTopBar(
            onBack = if (state == LobbyState.Idle || !isAvailable) onBack else onCancel,
            title = mode.title,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when {
                    !isAvailable -> Text(stringResource(R.string.online_unavailable), style = MaterialTheme.typography.bodyLarge)
                    state == LobbyState.Idle -> when (mode) {
                        OnlineLobbyMode.RANDOM -> QuickMatchCard(onQuickMatch)
                        OnlineLobbyMode.FRIEND -> {
                            CreateGameCard(onCreateGame)
                            JoinGameCard(onJoin)
                        }
                    }
                    state is LobbyState.WaitingForFriend -> WaitingForFriend(state.code, state.side, onShareCode, onCancel)
                    else -> Busy(state, onCancel)
                }
            }
        }
    }
}

/** Tiger against goat, on the primary container: the main way in. */
@Composable
private fun QuickMatchCard(onQuickMatch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.primaryContainer)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VersusArt(pieceCircle = 96.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.online_quick_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.online_quick_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        PrimaryButton(
            text = stringResource(R.string.online_find),
            onClick = onQuickMatch,
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.ic_search,
        )
    }
}

/** Decorative: hidden from TalkBack, the card's title says what it is. */
@Composable
private fun VersusArt(pieceCircle: Dp) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PieceCircle(LocalPieceArt.current.tiger, pieceCircle, colors.surface)
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.online_vs), color = colors.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        PieceCircle(LocalPieceArt.current.goat, pieceCircle, colors.surface)
    }
}

@Composable
internal fun PieceCircle(@DrawableRes piece: Int, size: Dp, background: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Image(painterResource(piece), contentDescription = null, modifier = Modifier.size(size * 0.8f))
    }
}

@Composable
private fun FriendCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun FriendCardHeader(@DrawableRes icon: Int, @StringRes title: Int, @StringRes detail: Int) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onTertiaryContainer)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateGameCard(onCreateGame: (Side) -> Unit) {
    var side by rememberSaveable { mutableStateOf(Side.GOAT) }
    FriendCard {
        FriendCardHeader(R.drawable.ic_add, R.string.online_create, R.string.online_create_short)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SideChip(Side.GOAT, selected = side == Side.GOAT, onClick = { side = Side.GOAT }, modifier = Modifier.weight(1f))
            SideChip(Side.TIGER, selected = side == Side.TIGER, onClick = { side = Side.TIGER }, modifier = Modifier.weight(1f))
        }
        PrimaryButton(
            text = stringResource(R.string.online_create_button),
            onClick = { onCreateGame(side) },
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.ic_add,
        )
    }
}

/** Which side the host plays: a compact radio button in that side's colours. */
@Composable
private fun SideChip(side: Side, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isTiger = side == Side.TIGER
    val accent = if (isTiger) colors.primary else colors.secondary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (selected) (if (isTiger) colors.primaryContainer else colors.secondaryContainer) else colors.surfaceContainerLowest)
            .border(if (selected) BorderStroke(2.dp, accent) else BorderStroke(1.dp, colors.outlineVariant), shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Image(painterResource(if (isTiger) LocalPieceArt.current.tiger else LocalPieceArt.current.goat), contentDescription = null, modifier = Modifier.size(32.dp))
        Text(
            stringResource(if (isTiger) R.string.side_tigers else R.string.side_goats),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) (if (isTiger) colors.onPrimaryContainer else colors.onSecondaryContainer) else colors.onSurface,
        )
    }
}

@Composable
private fun JoinGameCard(onJoin: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    FriendCard {
        FriendCardHeader(R.drawable.ic_join, R.string.online_join_title, R.string.online_code_label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CodeField(code = code, onCodeChange = { code = it }, onDone = { onJoin(code) }, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(R.string.online_join), onClick = { onJoin(code) })
        }
    }
}

/** One digits-only text field drawn as a box per digit. */
@Composable
private fun CodeField(code: String, onCodeChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val label = stringResource(R.string.online_code_label)
    BasicTextField(
        value = code,
        onValueChange = { typed -> onCodeChange(typed.filter(Char::isDigit).take(RoomCode.LENGTH)) },
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onDone() }),
        modifier = modifier.semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(RoomCode.LENGTH) { i ->
                        val digit = code.getOrNull(i)
                        val active = digit != null || (focused && i == code.length)
                        val shape = RoundedCornerShape(14.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(shape)
                                .background(colors.surfaceContainerLowest)
                                .border(2.dp, if (active) colors.tertiary else colors.outlineVariant, shape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(digit?.toString().orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // The real field stays invisible on top, so taps and the cursor still work.
                Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
            }
        },
    )
}

@Composable
private fun WaitingForFriend(code: String, side: Side, onShareCode: (String) -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copiedText = stringResource(R.string.online_copied)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.online_your_code).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.online_code_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    // Spaced out for TalkBack, so it reads digit by digit.
    val spaced = code.toList().joinToString(" ")
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spaced },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        code.forEach { digit ->
            Box(
                Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(digit.toString(), fontSize = 52.sp, fontWeight = FontWeight.Black, color = colors.onPrimaryContainer)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(
            text = stringResource(R.string.online_share),
            onClick = { onShareCode(code) },
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_share,
        )
        OutlinedIconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(copiedText, code)))
                    // Android 13+ confirms copies itself.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.size(56.dp),
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Icon(painterResource(R.drawable.ic_copy), contentDescription = stringResource(R.string.online_copy), tint = colors.primary)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val isTiger = side == Side.TIGER
            PieceCircle(
                if (isTiger) LocalPieceArt.current.tiger else LocalPieceArt.current.goat,
                72.dp,
                colors.surface,
                Modifier.border(3.dp, if (isTiger) colors.primary else colors.secondary, CircleShape),
            )
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Box(
                Modifier.size(72.dp).border(BorderStroke(2.dp, colors.outline), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_person), contentDescription = null, tint = colors.outline, modifier = Modifier.size(32.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.online_waiting_friend),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                stringResource(R.string.online_keep_open),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.online_cancel_game))
    }
}

/** Searching, creating or joining: the hero card with a spinner in place of the button. */
@Composable
private fun Busy(state: LobbyState, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val text = when (state) {
        LobbyState.Searching -> R.string.online_searching
        LobbyState.Creating -> R.string.online_creating
        else -> R.string.online_joining
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            VersusArt(pieceCircle = 96.dp)
            CircularProgressIndicator()
            Text(
                stringResource(text),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            SecondaryButton(stringResource(R.string.cancel), onCancel)
        }
        if (state == LobbyState.Searching) {
            SearchTimer(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 18.dp))
        }
    }
}

/** Time spent looking for an opponent, as m:ss, counting up from when the search started. */
@Composable
private fun SearchTimer(modifier: Modifier = Modifier) {
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            seconds++
        }
    }
    Text(
        stringResource(R.string.online_search_time, seconds / 60, seconds % 60),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun shareCodeIntent(text: String): Intent =
    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)

private val LobbyMessage.text: Int
    get() = when (this) {
        LobbyMessage.CONNECTION -> R.string.online_error_connection
        LobbyMessage.CODE_NOT_FOUND -> R.string.online_error_code
        LobbyMessage.GAME_FULL -> R.string.online_error_full
        LobbyMessage.BAD_CODE -> R.string.online_error_bad_code
    }

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun RandomLobbyPreview() {
    BaghchalTheme {
        OnlineLobbyContent(
            mode = OnlineLobbyMode.RANDOM,
            isAvailable = true,
            state = LobbyState.Idle,
            onQuickMatch = {},
            onCreateGame = {},
            onJoin = {},
            onCancel = {},
            onShareCode = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun FriendLobbyPreview() {
    BaghchalTheme {
        OnlineLobbyContent(
            mode = OnlineLobbyMode.FRIEND,
            isAvailable = true,
            state = LobbyState.Idle,
            onQuickMatch = {},
            onCreateGame = {},
            onJoin = {},
            onCancel = {},
            onShareCode = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun WaitingForFriendPreview() {
    BaghchalTheme {
        OnlineLobbyContent(
            mode = OnlineLobbyMode.FRIEND,
            isAvailable = true,
            state = LobbyState.WaitingForFriend("4821", Side.GOAT),
            onQuickMatch = {},
            onCreateGame = {},
            onJoin = {},
            onCancel = {},
            onShareCode = {},
            onBack = {},
        )
    }
}
