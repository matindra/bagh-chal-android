package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import com.matinstudios.baghchal.data.Entitlements
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.Settings
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
        onMusicChanged = viewModel::onMusicChanged,
        onHapticsChanged = viewModel::onHapticsChanged,
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
    onMusicChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
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
                ToggleRow(R.string.settings_music, R.string.settings_music_detail, settings.musicEnabled, onMusicChanged)
                HorizontalDivider()
                ToggleRow(R.string.settings_haptics, R.string.settings_haptics_detail, settings.hapticsEnabled, onHapticsChanged)
            }
            RemoveAdsCard(uiState, onRemoveAds, onRestorePurchase)
            message?.let {
                Text(stringResource(it.text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            if (uiState.privacyOptionsRequired) {
                QuietButton(stringResource(R.string.settings_privacy_options), onClick = onPrivacyOptions)
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
                QuietButton(
                    stringResource(R.string.settings_restore_purchase),
                    onClick = onRestorePurchase,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

private val SettingsMessage.text: Int
    get() = when (this) {
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
        trailingContent = {
            // White knob on a coral track, like Home's main button.
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HomeNight.Coral,
                    checkedBorderColor = HomeNight.Coral,
                ),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    BaghchalTheme {
        SettingsContent(
            SettingsUiState(Settings(), Entitlements(), "$1.99", purchasePending = false, privacyOptionsRequired = true),
            message = null,
            onSoundChanged = {},
            onMusicChanged = {},
            onHapticsChanged = {},
            onRemoveAds = {},
            onRestorePurchase = {},
            onPrivacyOptions = {},
            onBack = {},
        )
    }
}
