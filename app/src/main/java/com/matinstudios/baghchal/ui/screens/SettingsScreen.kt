package com.matinstudios.baghchal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.AdFree
import com.matinstudios.baghchal.data.Entitlements
import com.matinstudios.baghchal.data.Settings
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Each setting has its own colour, like Home's mode rows. */
private val SoundColors = ChipColors.Sky
private val MusicColors = ChipColors.Violet
private val HapticsColors = ChipColors.Pink
private val PrivacyColors = ChipColors.Green
private val GoldColors = ChipColors.Gold

/** Colour of a switched-off chip and track. */
private val OffChip = Color(0xFF55607A)
private val OffChipLip = Color(0xFF2E3548)

@Composable
fun SettingsScreen(
    onGoAdFree: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShop: (() -> Unit)? = null,
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
        onGoAdFree = onGoAdFree,
        onRestorePurchase = viewModel::onRestorePurchase,
        onPrivacyOptions = viewModel::onPrivacyOptions,
        onBack = onBack,
        modifier = modifier,
        onShop = onShop,
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    message: SettingsMessage?,
    onSoundChanged: (Boolean) -> Unit,
    onMusicChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onGoAdFree: () -> Unit,
    onRestorePurchase: () -> Unit,
    onPrivacyOptions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShop: (() -> Unit)? = null,
) {
    val settings = uiState.settings
    // Same float and shine wave as Home's buttons; still when system animations are off.
    val clock = rememberSceneClock()
    Column(modifier.fillMaxSize()) {
        BackTopBar(onBack, onShop = onShop)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsTitle()
            val rowModifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            SettingToggle(
                R.string.settings_sound, R.string.settings_sound_detail, R.drawable.ic_sound_on,
                SoundColors, settings.soundEnabled, onSoundChanged, clock, 0, rowModifier,
            )
            SettingToggle(
                R.string.settings_music, R.string.settings_music_detail, R.drawable.ic_music,
                MusicColors, settings.musicEnabled, onMusicChanged, clock, 1, rowModifier,
            )
            SettingToggle(
                R.string.settings_haptics, R.string.settings_haptics_detail, R.drawable.ic_vibration,
                HapticsColors, settings.hapticsEnabled, onHapticsChanged, clock, 2, rowModifier,
            )
            RemoveAdsCard(uiState, onGoAdFree, onRestorePurchase, rowModifier.padding(top = 6.dp))
            message?.let {
                Text(
                    stringResource(it.text),
                    color = HomeNight.Link,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            if (uiState.privacyOptionsRequired) {
                FrostModeButton(
                    title = R.string.settings_privacy_options,
                    detail = R.string.settings_privacy_options_detail,
                    icon = R.drawable.ic_privacy,
                    colors = PrivacyColors,
                    clock = clock,
                    index = 3,
                    onClick = onPrivacyOptions,
                    modifier = rowModifier,
                )
            }
        }
    }
}

/** Big chunky title with a gold gear chip, like a game menu header. */
@Composable
private fun SettingsTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        IconChip(R.drawable.ic_settings, HomeNight.Gold, HomeNight.GoldLip, HomeNight.OnGold, size = 44)
        BasicText(
            stringResource(R.string.settings_title),
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                shadow = Shadow(HomeNight.VioletLip, with(LocalDensity.current) { Offset(0f, 3.dp.toPx()) }, 0f),
            ),
        )
    }
}

/** A raised square chip with an icon, sitting on its darker lip. */
@Composable
internal fun IconChip(@DrawableRes icon: Int, color: Color, lip: Color, tint: Color, size: Int = 40) {
    Box(
        Modifier
            .size(size.dp)
            .drawBehind {
                val r = CornerRadius(12.dp.toPx())
                drawRoundRect(lip, Offset(0f, 3.dp.toPx()), this.size, r)
                drawRoundRect(color, cornerRadius = r)
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.56f).dp))
    }
}

/**
 * A setting as a tinted-glass row that floats and shines like Home's mode rows. The whole row
 * toggles; its colour glows when on and greys out when off.
 */
@Composable
private fun SettingToggle(
    @StringRes title: Int,
    @StringRes detail: Int,
    @DrawableRes icon: Int,
    colors: ModeColors,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    clock: State<Long>,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val chip by animateColorAsState(if (checked) colors.chip else OffChip, label = "settingChip")
    val chipLip by animateColorAsState(if (checked) colors.chipLip else OffChipLip, label = "settingChipLip")
    val state = stringResource(if (checked) R.string.settings_on else R.string.settings_off)
    RaisedButton(
        onClick = { onChange(!checked) },
        face = chip.copy(alpha = if (checked) 0.2f else 0.1f),
        lip = HomeNight.FrostLip,
        shape = RoundedCornerShape(18.dp),
        lipHeight = 5.dp,
        role = Role.Switch,
        modifier = modifier
            .iceFloat(clock, index)
            .semantics {
                toggleableState = ToggleableState(checked)
                stateDescription = state
            },
        faceModifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        border = chip.copy(alpha = if (checked) 0.6f else 0.35f),
        decoration = { if (checked) drawShineWave(clock, index) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconChip(icon, chip, chipLip, Color.White)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    stringResource(title),
                    style = TextStyle(color = HomeNight.OnFrost, fontWeight = FontWeight.Black),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 19.sp),
                )
                BasicText(
                    stringResource(detail),
                    style = TextStyle(
                        color = if (checked) colors.ink else HomeNight.OnFrostSoft,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    ),
                )
            }
            ChunkySwitch(checked, chip, chipLip)
        }
    }
}

/** A fat game-style switch: a coloured track on a lip with a white knob that slides across. */
@Composable
private fun ChunkySwitch(checked: Boolean, track: Color, trackLip: Color) {
    val knobX by animateDpAsState(if (checked) 24.dp else 0.dp, label = "switchKnob")
    Box(
        Modifier
            .size(width = 54.dp, height = 30.dp)
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(trackLip, Offset(0f, 3.dp.toPx()), size, r)
                drawRoundRect(track, cornerRadius = r)
            }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(24.dp)
                .drawBehind { drawCircle(Color.Black.copy(alpha = 0.25f), center = center + Offset(0f, 2.dp.toPx())) }
                .background(Color.White, CircleShape),
        )
    }
}

/** Gold-tinted glass panel with the ad-free offer, or the thank-you once it is owned for life. */
@Composable
private fun RemoveAdsCard(
    uiState: SettingsUiState,
    onGoAdFree: () -> Unit,
    onRestorePurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifetime = uiState.adFree == AdFree.LIFETIME
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(GoldColors.chip.copy(alpha = 0.14f))
            .border(1.5.dp, GoldColors.chip.copy(alpha = 0.55f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconChip(R.drawable.ic_remove_ads, HomeNight.Gold, HomeNight.GoldLip, HomeNight.OnGold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    stringResource(R.string.settings_remove_ads),
                    style = TextStyle(color = HomeNight.OnFrost, fontWeight = FontWeight.Black, fontSize = 19.sp),
                )
                BasicText(
                    adFreeStatus(uiState.adFree),
                    style = TextStyle(color = GoldColors.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp),
                )
            }
        }
        if (!lifetime) {
            PrimaryButton(stringResource(R.string.settings_remove_ads), onClick = onGoAdFree, modifier = Modifier.fillMaxWidth())
            QuietButton(
                stringResource(R.string.settings_restore_purchase),
                onClick = onRestorePurchase,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private val SettingsMessage.text: Int
    get() = when (this) {
        SettingsMessage.RESTORED -> R.string.message_restored
        SettingsMessage.NOT_OWNED -> R.string.message_not_owned
        SettingsMessage.RESTORE_FAILED -> R.string.message_restore_failed
    }

@Preview(showBackground = true, backgroundColor = 0xFF101A3A)
@Composable
private fun SettingsContentPreview() {
    BaghchalTheme {
        SettingsContent(
            SettingsUiState(Settings(), Entitlements(), AdFree.NONE, privacyOptionsRequired = true),
            message = null,
            onSoundChanged = {},
            onMusicChanged = {},
            onHapticsChanged = {},
            onGoAdFree = {},
            onRestorePurchase = {},
            onPrivacyOptions = {},
            onBack = {},
        )
    }
}
