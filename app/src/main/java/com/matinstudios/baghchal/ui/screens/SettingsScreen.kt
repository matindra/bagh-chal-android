package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val settings = viewModel.settings.collectAsStateWithLifecycle().value ?: return
    SettingsContent(
        settings = settings,
        onSoundChanged = viewModel::onSoundChanged,
        onHapticsChanged = viewModel::onHapticsChanged,
        onBoardThemeSelected = viewModel::onBoardThemeSelected,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    settings: Settings,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onBoardThemeSelected: (BoardTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
                            selected = theme == settings.boardTheme,
                            onSelect = { onBoardThemeSelected(theme) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
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
private fun ThemeOption(theme: BoardTheme, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painterResource(theme.drawable),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(3.dp, borderColor, MaterialTheme.shapes.small)
                .padding(5.dp),
        )
        Text(
            stringResource(theme.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val BoardTheme.label: Int
    get() = when (this) {
        BoardTheme.WOOD -> R.string.board_wood
        BoardTheme.STONE -> R.string.board_stone
        BoardTheme.CLOTH -> R.string.board_cloth
    }

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    BaghchalTheme {
        SettingsContent(
            Settings(),
            onSoundChanged = {},
            onHapticsChanged = {},
            onBoardThemeSelected = {},
            onBack = {},
        )
    }
}
