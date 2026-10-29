package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.AppIcon
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.Entitlements
import com.matinstudios.baghchal.data.PieceSet
import com.matinstudios.baghchal.ui.board.art
import com.matinstudios.baghchal.ui.board.drawable
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** What the Shop offers; each opens its own picker. */
enum class ShopSection(@StringRes val title: Int) {
    BOARDS(R.string.shop_boards),
    PIECES(R.string.shop_pieces),
    APP_ICONS(R.string.shop_app_icons),
}

/** One Shop section's picker: boards, piece sets or app icons. */
@Composable
fun BoardsScreen(
    section: ShopSection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardsViewModel = viewModel(factory = BoardsViewModel.Factory),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val noAdAvailable by viewModel.noAdAvailable.collectAsStateWithLifecycle()
    val appIcon by viewModel.appIcon.collectAsStateWithLifecycle()
    BoardsContent(
        section = section,
        uiState = uiState,
        noAdAvailable = noAdAvailable,
        onBoardSelected = viewModel::onBoardSelected,
        onBack = onBack,
        modifier = modifier,
        appIcon = appIcon,
        onPieceSetSelected = viewModel::onPieceSetSelected,
        onAppIconSelected = viewModel::onAppIconSelected,
    )
}

@Composable
fun BoardsContent(
    section: ShopSection,
    uiState: BoardsUiState,
    noAdAvailable: Boolean,
    onBoardSelected: (BoardTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    appIcon: AppIcon = AppIcon.CLASSIC,
    onPieceSetSelected: (PieceSet) -> Unit = {},
    onAppIconSelected: (AppIcon) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        BackTopBar(onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(section.title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (noAdAvailable) {
                Text(stringResource(R.string.message_no_ad), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            val sectionModifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            when (section) {
                ShopSection.BOARDS -> OptionGrid(BoardTheme.entries, sectionModifier) { theme, tileModifier ->
                    ChoiceTile(
                        label = theme.label,
                        selected = theme == uiState.selected,
                        locked = !uiState.entitlements.isUnlocked(theme),
                        onSelect = { onBoardSelected(theme) },
                        modifier = tileModifier,
                    ) {
                        Image(painterResource(theme.drawable), contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                }
                ShopSection.PIECES -> OptionGrid(PieceSet.entries, sectionModifier) { set, tileModifier ->
                    ChoiceTile(
                        label = set.label,
                        selected = set == uiState.selectedPieces,
                        locked = !uiState.entitlements.isUnlocked(set),
                        onSelect = { onPieceSetSelected(set) },
                        modifier = tileModifier,
                    ) {
                        PiecePairPicture(set, uiState.selected)
                    }
                }
                ShopSection.APP_ICONS -> {
                    Text(
                        stringResource(R.string.boards_app_icon_note),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = sectionModifier,
                    )
                    OptionGrid(AppIcon.entries, sectionModifier) { icon, tileModifier ->
                        ChoiceTile(
                            label = icon.label,
                            selected = icon == appIcon,
                            locked = false,
                            onSelect = { onAppIconSelected(icon) },
                            modifier = tileModifier,
                            shape = CircleShape,
                        ) {
                            LauncherIconPicture(icon)
                        }
                    }
                }
            }
        }
    }
}

/** A radio group laid out [OPTIONS_PER_ROW] to a row, every tile the same size. */
@Composable
private fun <T> OptionGrid(items: List<T>, modifier: Modifier, tile: @Composable (T, Modifier) -> Unit) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in items.chunked(OPTIONS_PER_ROW)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (item in row) tile(item, Modifier.weight(1f))
                // Keep a short last row at the same tile size.
                repeat(OPTIONS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChoiceTile(
    @StringRes label: Int,
    selected: Boolean,
    locked: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    picture: @Composable BoxScope.() -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(3.dp, borderColor, shape)
                    .padding(5.dp)
                    .clip(shape)
                    .alpha(if (locked) LOCKED_ALPHA else 1f),
                content = picture,
            )
            if (locked) {
                Image(
                    painterResource(R.drawable.lock_badge),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(28.dp),
                )
            }
        }
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        // Tapping a locked option plays a rewarded ad, so say so before the tap.
        if (locked) {
            Text(
                stringResource(R.string.boards_watch_ad),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The set's tiger and goat side by side on the board in use, as they will look in a game. */
@Composable
internal fun PiecePairPicture(set: PieceSet, board: BoardTheme) {
    Image(painterResource(board.drawable), contentDescription = null, modifier = Modifier.fillMaxSize())
    Row(Modifier.fillMaxSize().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(set.art.tiger), contentDescription = null, modifier = Modifier.weight(1f).aspectRatio(1f))
        Image(painterResource(set.art.goat), contentDescription = null, modifier = Modifier.weight(1f).aspectRatio(1f))
    }
}

/** The adaptive icon's two layers, scaled so the launcher's visible 72 of 108 units fill the circle. */
@Composable
internal fun LauncherIconPicture(icon: AppIcon) {
    val layers = Modifier.fillMaxSize().scale(ADAPTIVE_ICON_ZOOM)
    Image(painterResource(icon.background), contentDescription = null, modifier = layers)
    Image(painterResource(icon.foreground), contentDescription = null, modifier = layers)
}

@get:StringRes
val BoardTheme.label: Int
    get() = when (this) {
        BoardTheme.WOOD -> R.string.board_wood
        BoardTheme.STONE -> R.string.board_stone
        BoardTheme.CLOTH -> R.string.board_cloth
        BoardTheme.NIGHT -> R.string.board_night
        BoardTheme.THANGKA -> R.string.board_thangka
        BoardTheme.JUNGLE -> R.string.board_jungle
        BoardTheme.MANDALA -> R.string.board_mandala
        BoardTheme.DHAKA -> R.string.board_dhaka
        BoardTheme.BRASS -> R.string.board_brass
        BoardTheme.INK -> R.string.board_ink
        BoardTheme.SNOW -> R.string.board_snow
    }

@get:StringRes
val PieceSet.label: Int
    get() = when (this) {
        PieceSet.CLASSIC -> R.string.piece_set_classic
        PieceSet.TOKEN -> R.string.piece_set_token
        PieceSet.GLYPH -> R.string.piece_set_glyph
        PieceSet.FESTIVAL -> R.string.piece_set_festival
        PieceSet.SNOW -> R.string.piece_set_snow
        PieceSet.NIGHT_EYES -> R.string.piece_set_night_eyes
    }

@get:StringRes
private val AppIcon.label: Int
    get() = when (this) {
        AppIcon.CLASSIC -> R.string.app_icon_classic
        AppIcon.FACE_OFF -> R.string.app_icon_face_off
        AppIcon.GLYPH -> R.string.app_icon_glyph
        AppIcon.BOARD -> R.string.app_icon_board
        AppIcon.NIGHT -> R.string.app_icon_night
        AppIcon.MASK -> R.string.app_icon_mask
    }

@get:DrawableRes
private val AppIcon.background: Int
    get() = when (this) {
        AppIcon.CLASSIC -> R.drawable.ic_launcher_background
        AppIcon.FACE_OFF -> R.drawable.ic_launcher_faceoff_background
        AppIcon.GLYPH -> R.drawable.ic_launcher_glyph_background
        AppIcon.BOARD -> R.drawable.ic_launcher_board_background
        AppIcon.NIGHT -> R.drawable.ic_launcher_night_background
        AppIcon.MASK -> R.drawable.ic_launcher_mask_background
    }

@get:DrawableRes
private val AppIcon.foreground: Int
    get() = when (this) {
        AppIcon.CLASSIC -> R.drawable.ic_launcher_foreground
        AppIcon.FACE_OFF -> R.drawable.ic_launcher_faceoff_foreground
        AppIcon.GLYPH -> R.drawable.ic_launcher_glyph_foreground
        AppIcon.BOARD -> R.drawable.ic_launcher_board_foreground
        AppIcon.NIGHT -> R.drawable.ic_launcher_night_foreground
        AppIcon.MASK -> R.drawable.ic_launcher_mask_foreground
    }

private const val LOCKED_ALPHA = 0.6f
private const val OPTIONS_PER_ROW = 3
private const val ADAPTIVE_ICON_ZOOM = 108f / 72f

@Preview(showBackground = true)
@Composable
private fun BoardsContentPreview() {
    BaghchalTheme {
        BoardsContent(
            ShopSection.PIECES,
            BoardsUiState(
                BoardTheme.WOOD,
                Entitlements(unlockedThemes = setOf(BoardTheme.NIGHT), unlockedPieceSets = setOf(PieceSet.TOKEN)),
                PieceSet.TOKEN,
            ),
            noAdAvailable = false,
            onBoardSelected = {},
            onBack = {},
            appIcon = AppIcon.FACE_OFF,
        )
    }
}
