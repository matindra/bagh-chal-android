package com.matinstudios.baghchal.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matinstudios.baghchal.BuildConfig
import com.matinstudios.baghchal.R
import com.matinstudios.baghchal.billing.AdFreeProduct
import com.matinstudios.baghchal.billing.YEARLY_PASS_PRODUCT_ID
import com.matinstudios.baghchal.data.AdFree
import com.matinstudios.baghchal.ui.theme.BaghchalTheme

/** Where Play lets the player cancel or change the yearly pass. */
private val MANAGE_SUBSCRIPTION_URL =
    "https://play.google.com/store/account/subscriptions?sku=$YEARLY_PASS_PRODUCT_ID&package=${BuildConfig.APPLICATION_ID}"

/** Go ad-free: a year by subscription, or for good. */
@Composable
fun AdFreeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShop: (() -> Unit)? = null,
    viewModel: AdFreeViewModel = viewModel(factory = AdFreeViewModel.Factory),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value ?: return
    val message by viewModel.message.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AdFreeContent(
        uiState = uiState,
        message = message,
        onBuy = viewModel::onBuy,
        onManageSubscription = { runCatching { uriHandler.openUri(MANAGE_SUBSCRIPTION_URL) } },
        onRestorePurchase = viewModel::onRestorePurchase,
        onBack = onBack,
        modifier = modifier,
        onShop = onShop,
    )
}

@Composable
fun AdFreeContent(
    uiState: AdFreeUiState,
    message: AdFreeMessage?,
    onBuy: (AdFreeProduct) -> Unit,
    onManageSubscription: () -> Unit,
    onRestorePurchase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShop: (() -> Unit)? = null,
) {
    val adFree = uiState.adFree
    Column(modifier.fillMaxSize()) {
        BackTopBar(onBack, onShop = onShop)
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
                stringResource(R.string.ad_free_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                adFreeStatus(adFree),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            val cardModifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
            when (adFree) {
                AdFree.NONE -> OptionCard(R.string.ad_free_year_title, R.string.ad_free_year_detail, cardModifier) {
                    BuyButton(AdFreeProduct.YEARLY, uiState, onBuy, primary = false)
                }
                AdFree.YEARLY -> OptionCard(R.string.ad_free_year_title, R.string.ad_free_year_active, cardModifier) {
                    QuietButton(
                        stringResource(R.string.ad_free_manage),
                        onClick = onManageSubscription,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                AdFree.LIFETIME -> Unit
            }
            if (adFree != AdFree.LIFETIME) {
                OptionCard(R.string.ad_free_lifetime_title, R.string.ad_free_lifetime_detail, cardModifier) {
                    BuyButton(AdFreeProduct.LIFETIME, uiState, onBuy, primary = true)
                }
            }
            message?.let {
                Text(stringResource(it.text), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            if (adFree != AdFree.LIFETIME) {
                QuietButton(stringResource(R.string.settings_restore_purchase), onClick = onRestorePurchase)
            }
        }
    }
}

/** One sentence on how the player is ad-free now; also used by Settings. */
@Composable
fun adFreeStatus(adFree: AdFree): String = when (adFree) {
    AdFree.NONE -> stringResource(R.string.ad_free_intro)
    AdFree.YEARLY -> stringResource(R.string.ad_free_year_active)
    AdFree.LIFETIME -> stringResource(R.string.settings_remove_ads_owned)
}

@Composable
private fun OptionCard(
    @StringRes title: Int,
    @StringRes detail: Int,
    modifier: Modifier = Modifier,
    action: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action()
        }
    }
}

/** Buy with Play's price on it; a pending payment shows a note instead. */
@Composable
private fun ColumnScope.BuyButton(product: AdFreeProduct, uiState: AdFreeUiState, onBuy: (AdFreeProduct) -> Unit, primary: Boolean) {
    if (product in uiState.pending) {
        Text(stringResource(R.string.ad_free_pending), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val price = uiState.prices[product]
    val text = if (price == null) stringResource(R.string.ad_free_buy) else stringResource(R.string.ad_free_buy_price, price)
    if (primary) {
        PrimaryButton(text, onClick = { onBuy(product) }, modifier = Modifier.fillMaxWidth())
    } else {
        SecondaryButton(text, onClick = { onBuy(product) }, modifier = Modifier.fillMaxWidth())
    }
}

private val AdFreeMessage.text: Int
    get() = when (this) {
        AdFreeMessage.PURCHASE_UNAVAILABLE -> R.string.message_purchase_unavailable
        AdFreeMessage.RESTORED -> R.string.message_restored
        AdFreeMessage.NOT_OWNED -> R.string.message_not_owned
        AdFreeMessage.RESTORE_FAILED -> R.string.message_restore_failed
    }

@Preview(showBackground = true)
@Composable
private fun AdFreeContentPreview() {
    BaghchalTheme {
        AdFreeContent(
            uiState = AdFreeUiState(
                adFree = AdFree.NONE,
                prices = mapOf(AdFreeProduct.YEARLY to "₹49.00", AdFreeProduct.LIFETIME to "₹199.00"),
                pending = emptySet(),
            ),
            message = null,
            onBuy = {},
            onManageSubscription = {},
            onRestorePurchase = {},
            onBack = {},
        )
    }
}
