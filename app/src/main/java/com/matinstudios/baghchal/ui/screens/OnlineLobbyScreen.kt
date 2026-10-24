package com.matinstudios.baghchal.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.engine.GOATS_TO_WIN
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.online.RoomCode
import com.matinstudios.baghchal.ui.theme.BaghchalTheme
import com.matinstudios.baghchal.ui.theme.PrayerFlags

/** Play online: a quick match against anyone, or a private game with a 4-digit code. */
@Composable
fun OnlineLobbyScreen(
    onMatched: (roomId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineLobbyViewModel = viewModel(factory = OnlineLobbyViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnMatched by rememberUpdatedState(onMatched)
    LaunchedEffect(viewModel) { viewModel.matched.collect { currentOnMatched(it) } }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(context.getString(it.text)) }
    }
    Column(modifier.fillMaxSize()) {
        OnlineLobbyContent(
            isAvailable = viewModel.isAvailable,
            state = state,
            onQuickMatch = viewModel::onQuickMatch,
            onCreateGame = viewModel::onCreateGame,
            onJoin = viewModel::onJoin,
            onCancel = viewModel::onCancel,
            onShareCode = { code -> context.startActivity(shareCodeIntent(context.getString(R.string.online_share_text, code))) },
            onBack = onBack,
            modifier = Modifier.weight(1f),
        )
        SnackbarHost(snackbar)
    }
}

@Composable
fun OnlineLobbyContent(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PrayerFlags()
        Text(
            stringResource(R.string.mode_online),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                !isAvailable -> Text(stringResource(R.string.online_unavailable), style = MaterialTheme.typography.bodyLarge)
                state == LobbyState.Idle -> {
                    QuickMatchCard(onQuickMatch)
                    FriendCard(onCreateGame, onJoin)
                }
                state is LobbyState.WaitingForFriend -> WaitingForFriend(state.code, onShareCode, onCancel)
                else -> Busy(state, onCancel)
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun QuickMatchCard(onQuickMatch: () -> Unit) {
    LobbyCard {
        Text(stringResource(R.string.online_quick_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.online_quick_detail), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onQuickMatch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.online_find)) }
    }
}

@Composable
private fun FriendCard(onCreateGame: (Side) -> Unit, onJoin: (String) -> Unit) {
    var side by rememberSaveable { mutableStateOf(Side.GOAT) }
    var code by rememberSaveable { mutableStateOf("") }
    LobbyCard {
        Text(stringResource(R.string.online_friend_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.online_create_detail), style = MaterialTheme.typography.bodyMedium)
        SectionLabel(R.string.home_play_as)
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SideOption(
                side = Side.GOAT,
                selected = side == Side.GOAT,
                onClick = { side = Side.GOAT },
                detail = stringResource(R.string.side_goats_detail),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            SideOption(
                side = Side.TIGER,
                selected = side == Side.TIGER,
                onClick = { side = Side.TIGER },
                detail = stringResource(R.string.side_tigers_detail, GOATS_TO_WIN),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Button(onClick = { onCreateGame(side) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.online_create)) }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionLabel(R.string.online_join_title)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { typed -> code = typed.filter(Char::isDigit).take(RoomCode.LENGTH) },
                label = { Text(stringResource(R.string.online_code_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onJoin(code) }),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { onJoin(code) }) { Text(stringResource(R.string.online_join)) }
        }
    }
}

@Composable
private fun WaitingForFriend(code: String, onShareCode: (String) -> Unit, onCancel: () -> Unit) {
    LobbyCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.online_your_code), style = MaterialTheme.typography.titleMedium)
        // Spaced out so it's easy to read aloud; TalkBack reads it digit by digit.
        val spaced = code.toList().joinToString(" ")
        Text(
            spaced,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = spaced },
        )
        Button(onClick = { onShareCode(code) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.online_share)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.online_waiting_friend),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun Busy(state: LobbyState, onCancel: () -> Unit) {
    val text = when (state) {
        LobbyState.Searching -> R.string.online_searching
        LobbyState.Creating -> R.string.online_creating
        else -> R.string.online_joining
    }
    LobbyCard(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(
            stringResource(text),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun LobbyCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
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

@Preview(showBackground = true)
@Composable
private fun OnlineLobbyPreview() {
    BaghchalTheme {
        OnlineLobbyContent(
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
