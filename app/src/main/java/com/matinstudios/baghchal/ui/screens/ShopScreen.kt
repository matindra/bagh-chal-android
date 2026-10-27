package com.matinstudios.baghchal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.AppIcon
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.Entitlements
import com.matinstudios.baghchal.data.PieceSet
import com.matinstudios.baghchal.ui.board.drawable
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** The Shop, opened from the side menu: one row per [ShopSection], each showing what is in use. */
@Composable
fun ShopScreen(
    onOpenSection: (ShopSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardsViewModel = viewModel(factory = BoardsViewModel.Factory),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val appIcon by viewModel.appIcon.collectAsStateWithLifecycle()
    ShopContent(uiState, appIcon, onOpenSection, onBack, modifier)
}

@Composable
fun ShopContent(
    uiState: BoardsUiState,
    appIcon: AppIcon,
    onOpenSection: (ShopSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val owned = uiState.entitlements
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
                stringResource(R.string.shop_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            val rowModifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            ShopRow(
                section = ShopSection.BOARDS,
                detail = unlockedCount(BoardTheme.entries.count(owned::isUnlocked), BoardTheme.entries.size),
                onOpen = onOpenSection,
                modifier = rowModifier,
            ) {
                Image(painterResource(uiState.selected.drawable), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            ShopRow(
                section = ShopSection.PIECES,
                detail = unlockedCount(PieceSet.entries.count(owned::isUnlocked), PieceSet.entries.size),
                onOpen = onOpenSection,
                modifier = rowModifier,
            ) {
                PiecePairPicture(uiState.selectedPieces, uiState.selected)
            }
            ShopRow(
                section = ShopSection.APP_ICONS,
                detail = stringResource(R.string.shop_all_free),
                onOpen = onOpenSection,
                modifier = rowModifier,
                pictureShape = CircleShape,
            ) {
                LauncherIconPicture(appIcon)
            }
        }
    }
}

@Composable
private fun unlockedCount(unlocked: Int, total: Int): String =
    if (unlocked == total) stringResource(R.string.shop_all_unlocked) else stringResource(R.string.shop_unlocked, unlocked, total)

@Composable
private fun ShopRow(
    section: ShopSection,
    detail: String,
    onOpen: (ShopSection) -> Unit,
    modifier: Modifier = Modifier,
    pictureShape: Shape = MaterialTheme.shapes.small,
    picture: @Composable BoxScope.() -> Unit,
) {
    Card(
        onClick = { onOpen(section) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(72.dp).clip(pictureShape), content = picture)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(section.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopContentPreview() {
    BaghchalTheme {
        ShopContent(
            BoardsUiState(BoardTheme.NIGHT, Entitlements(unlockedThemes = setOf(BoardTheme.NIGHT)), PieceSet.CLASSIC),
            appIcon = AppIcon.CLASSIC,
            onOpenSection = {},
            onBack = {},
        )
    }
}
