package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.matinstudios.baghchal.data.Entitlements
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.BoardTheme
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.ui.board.drawable
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val message by viewModel.message.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        message = message,
        onSoundChanged = viewModel::onSoundChanged,
        onHapticsChanged = viewModel::onHapticsChanged,
        onBoardThemeSelected = viewModel::onBoardThemeSelected,
        onRemoveAds = viewModel::onRemoveAds,
        onRestorePurchase = viewModel::onRestorePurchase,
        onPrivacyOptions = viewModel::onPrivacyOptions,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    message: SettingsMessage?,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onBoardThemeSelected: (BoardTheme) -> Unit,
    onRemoveAds: () -> Unit,
    onRestorePurchase: () -> Unit,
    onPrivacyOptions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = uiState.settings
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
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Card(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                ToggleRow(R.string.settings_sound, R.string.settings_sound_detail, settings.soundEnabled, onSoundChanged)
                HorizontalDivider()
                ToggleRow(R.string.settings_haptics, R.string.settings_haptics_detail, settings.hapticsEnabled, onHapticsChanged)
            }
            Card(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_board), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (theme in BoardTheme.entries) {
                            ThemeOption(
                                theme = theme,
                                selected = theme == uiState.boardTheme,
                                locked = !uiState.entitlements.isUnlocked(theme),
                                onSelect = { onBoardThemeSelected(theme) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            RemoveAdsCard(uiState, onRemoveAds, onRestorePurchase)
            message?.let {
                Text(stringResource(it.text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            if (uiState.privacyOptionsRequired) {
                TextButton(onClick = onPrivacyOptions) { Text(stringResource(R.string.settings_privacy_options)) }
            }
        }
    }
}

@Composable
private fun RemoveAdsCard(uiState: SettingsUiState, onRemoveAds: () -> Unit, onRestorePurchase: () -> Unit) {
    Card(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_remove_ads), style = MaterialTheme.typography.titleMedium)
            when {
                uiState.entitlements.removeAds -> Text(stringResource(R.string.settings_remove_ads_owned))
                uiState.purchasePending -> Text(stringResource(R.string.settings_remove_ads_pending))
                else -> {
                    Text(stringResource(R.string.settings_remove_ads_detail), style = MaterialTheme.typography.bodyMedium)
                    val price = uiState.removeAdsPrice
                    PrimaryButton(
                        text = if (price == null) {
                            stringResource(R.string.settings_remove_ads)
                        } else {
                            stringResource(R.string.settings_remove_ads_price, price)
                        },
                        onClick = onRemoveAds,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (!uiState.entitlements.removeAds) {
                TextButton(onClick = onRestorePurchase, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.settings_restore_purchase))
                }
            }
        }
    }
}

private val SettingsMessage.text: Int
    get() = when (this) {
        SettingsMessage.NO_AD_AVAILABLE -> R.string.message_no_ad
        SettingsMessage.PURCHASE_UNAVAILABLE -> R.string.message_purchase_unavailable
        SettingsMessage.RESTORED -> R.string.message_restored
        SettingsMessage.NOT_OWNED -> R.string.message_not_owned
        SettingsMessage.RESTORE_FAILED -> R.string.message_restore_failed
    }

@Composable
private fun ToggleRow(@StringRes title: Int, @StringRes detail: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(detail)) },
        // The whole row toggles; the switch itself is just the indicator.
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun ThemeOption(
    theme: BoardTheme,
    selected: Boolean,
    locked: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
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
            Image(
                painterResource(theme.drawable),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(3.dp, borderColor, MaterialTheme.shapes.small)
                    .padding(5.dp)
                    .alpha(if (locked) LOCKED_ALPHA else 1f),
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
            stringResource(theme.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        // Tapping a locked board plays a rewarded ad, so say so before the tap.
        if (locked) {
            Text(
                stringResource(R.string.settings_board_watch_ad),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val BoardTheme.label: Int
    get() = when (this) {
        BoardTheme.WOOD -> R.string.board_wood
        BoardTheme.STONE -> R.string.board_stone
        BoardTheme.CLOTH -> R.string.board_cloth
    }

private const val LOCKED_ALPHA = 0.6f

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    BaghchalTheme {
        SettingsContent(
            SettingsUiState(Settings(), Entitlements(), "$1.99", purchasePending = false, privacyOptionsRequired = true),
            message = null,
            onSoundChanged = {},
            onHapticsChanged = {},
            onBoardThemeSelected = {},
            onRemoveAds = {},
            onRestorePurchase = {},
            onPrivacyOptions = {},
            onBack = {},
        )
    }
}
