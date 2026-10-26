package com.matinstudios.baghchal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.BuildConfig
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.data.ThemeMode
import com.matinstudios.baghchal.ui.ThemeViewModel
import kotlinx.coroutines.launch

const val WEBSITE_URL = "https://matinstudios.com/bagh-chal-tiger-goat-game/"
const val PRIVACY_POLICY_URL = "https://matinstudios.com/bagh-chal-tiger-goat-game/privacy-policy"

/** one entry in the side menu. */
data class AppMenuItem(@StringRes val label: Int, @DrawableRes val icon: Int, val onClick: () -> Unit)

/**
 * Side menu (Home and game screens), opened by [AppMenuButton]. It covers only [content]; screens
 * keep their banner outside it so the menu never hides an ad.
 */
@Composable
fun AppMenuDrawer(
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory),
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    // Every screen's menu ends with the game's web page and privacy policy (open in the browser).
    val allItems = items + listOf(
        AppMenuItem(R.string.menu_website, R.drawable.ic_globe) { runCatching { uriHandler.openUri(WEBSITE_URL) } },
        AppMenuItem(R.string.menu_privacy_policy, R.drawable.ic_privacy) { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } },
    )
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(painterResource(R.drawable.ic_tiger), contentDescription = null, modifier = Modifier.size(36.dp))
                    Image(painterResource(R.drawable.ic_goat), contentDescription = null, modifier = Modifier.size(36.dp))
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                allItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.label)) },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        selected = false,
                        // Close first, so coming back to Home finds the menu shut.
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                item.onClick()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ThemePicker(
                    selected = themeMode ?: ThemeMode.SYSTEM,
                    onSelect = themeViewModel::onThemeModeChanged,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                )
                Text(
                    stringResource(R.string.menu_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        },
    ) {
        content { scope.launch { drawerState.open() } }
    }
}

/** System / Light / Dark choice at the bottom of the side menu. */
@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    Column(modifier) {
        Text(
            stringResource(R.string.menu_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(stringResource(label), maxLines = 1) },
                )
            }
        }
    }
}

/**
 * The top strip of every screen with the side menu: the menu button always sits in the same
 * top-left spot, with [content] (title, status) beside it.
 */
@Composable
fun MenuTopBar(onOpenMenu: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit = {}) {
    // The bar grows for tall content, but the button stays pinned to the top-left corner.
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = MenuTopBarHeight).padding(start = MenuButtonSpace),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        AppMenuButton(onOpenMenu, Modifier.padding(4.dp))
    }
}

/** Width the menu button takes from the left edge (button plus its margin). */
val MenuButtonSpace = 52.dp

val MenuTopBarHeight = 56.dp

@Composable
fun AppMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.home_menu))
    }
}
