package com.matinstudios.baghchal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.matinstudios.baghchal.R
import kotlinx.coroutines.launch

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
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
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
                items.forEach { item ->
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
            }
        },
    ) {
        content { scope.launch { drawerState.open() } }
    }
}

@Composable
fun AppMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.home_menu))
    }
}
