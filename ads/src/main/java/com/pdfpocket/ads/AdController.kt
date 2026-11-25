package com.pdfpocket.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.UserMessagingPlatform
import com.pdfpocket.data.Analytics

object AdController {
    // Google's official test IDs. Replace with real IDs before publishing.
    const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1030156715"
    const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val PRODUCT_ID = "remove_ads"
    private const val PREFS = "pdfpocket_prefs"
    private const val KEY_ADS_REMOVED = "ads_removed"
    private const val ACTIONS_BETWEEN = 2
    private const val MIN_SECONDS_BETWEEN = 120L

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var actions = 0
    private var lastShownMs = 0L
    private var billing: BillingClient? = null
    private var product: ProductDetails? = null

    fun initialize(activity: Activity) {
        MobileAds.initialize(activity) {}
        loadInterstitial(activity)
        loadRewarded(activity)
        requestConsent(activity)
        setupBilling(activity)
    }

    fun requestConsent(activity: Activity) {
        val params = com.google.android.ump.ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        UserMessagingPlatform.getConsentInformation(activity)
            .requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                        error?.let { android.util.Log.w("PDFPocketAds", "Consent: ${it.message}") }
                    }
                },
                { error -> android.util.Log.w("PDFPocketAds", "Consent update: ${error.message}") },
            )
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            },
        )
    }

    private fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
            },
        )
    }

    fun onUserAction() { actions++ }

    fun maybeShowInterstitial(activity: Activity) {
        if (isAdsRemoved(activity)) return
        val elapsed = System.currentTimeMillis() - lastShownMs
        if (actions < ACTIONS_BETWEEN || elapsed < MIN_SECONDS_BETWEEN * 1000) return
        interstitial?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitial = null
                    loadInterstitial(activity)
                }
            }
            ad.show(activity)
            lastShownMs = System.currentTimeMillis()
            actions = 0
            Analytics.log(Analytics.INTERSTITIAL_SHOWN)
        }
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        rewarded?.let { ad ->
            ad.show(activity) { onReward() }
            rewarded = null
            loadRewarded(activity)
            Analytics.log(Analytics.REWARDED_COMPLETED)
        } ?: onReward()
    }

    fun isAdsRemoved(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ADS_REMOVED, false)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun setupBilling(activity: Activity) {
        val listener = PurchasesUpdatedListener { result: BillingResult, purchases: List<Purchase>? ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID)) {
                        if (!purchase.isAcknowledged) {
                            billing?.acknowledgePurchase(
                                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                            ) {}
                        }
                        prefs(activity).edit().putBoolean(KEY_ADS_REMOVED, true).apply()
                        Analytics.log(Analytics.IAP_REMOVE_ADS)
                    }
                }
            }
        }
        val client = BillingClient.newBuilder(activity).setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        billing = client
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        ),
                    ).build()
                client.queryProductDetailsAsync(params) { _, result -> product = result.productDetailsList.firstOrNull() }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun launchRemoveAds(activity: Activity) {
        val details = product
        if (details == null) {
            Toast.makeText(activity, "Product not available (configure Play Console)", Toast.LENGTH_SHORT).show()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            ).build()
        billing?.launchBillingFlow(activity, params)
    }

    @Composable
    fun Banner() {
        val context = LocalContext.current
        if (isAdsRemoved(context)) return
        AndroidView(
            factory = {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_ID
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    fun SettingsPanel(activity: Activity) {
        var removed by remember { mutableStateOf(isAdsRemoved(activity)) }
        Text(if (removed) "Ads removed - thank you" else "Ads support this free app")
        Button(onClick = {
            showRewarded(activity) {
                Toast.makeText(activity, "Reward granted", Toast.LENGTH_SHORT).show()
            }
        }) { Text("Try a rewarded ad") }
        Button(onClick = { launchRemoveAds(activity) }) { Text("Remove ads (one-time)") }
        Button(onClick = {
            onUserAction(); onUserAction(); maybeShowInterstitial(activity)
        }) { Text("Show test interstitial") }
        Banner()
    }
}

