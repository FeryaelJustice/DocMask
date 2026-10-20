package com.feryaeljustice.docmask.data.repository

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.feryaeljustice.docmask.data.local.preferences.docMaskDataStore
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Offline-first billing repository handling Pro subscription/IAP state via Google Play IPC.
 * Persists Pro status in DataStore to guarantee 100% offline availability without network access.
 */
class BillingRepositoryImpl(
    private val context: Context,
    private val repositoryScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : BillingRepository, PurchasesUpdatedListener {

    companion object {
        val KEY_IS_PRO_USER = booleanPreferencesKey("key_is_pro_user")
        const val PRODUCT_ID_PRO = "docmask_pro"
    }

    private val billingClient: BillingClient by lazy {
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()
    }

    override val isProUser: Flow<Boolean> = context.docMaskDataStore.data.map { preferences ->
        preferences[KEY_IS_PRO_USER] ?: false
    }

    private suspend fun ensureConnected(): Boolean = withContext(Dispatchers.IO) {
        if (billingClient.isReady) return@withContext true

        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Handled automatically on next call
                }
            })
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                repositoryScope.launch {
                    refreshPurchases()
                }
            }
            else -> {
                // User cancelled or other non-fatal code
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        repositoryScope.launch {
            var hasActivePro = false
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    hasActivePro = true
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        try {
                            billingClient.acknowledgePurchase(ackParams)
                        } catch (_: Exception) {
                            // Offline or acknowledge error, will retry later
                        }
                    }
                }
            }
            if (hasActivePro) {
                saveProStatusToDataStore(true)
            }
        }
    }

    private suspend fun saveProStatusToDataStore(isPro: Boolean) {
        context.docMaskDataStore.edit { preferences ->
            preferences[KEY_IS_PRO_USER] = isPro
        }
    }

    override suspend fun refreshPurchases() = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) return@withContext

            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val subsResult = billingClient.queryPurchasesAsync(subsParams)
            val inAppResult = billingClient.queryPurchasesAsync(inAppParams)

            val allPurchases = (subsResult.purchasesList.orEmpty() + inAppResult.purchasesList.orEmpty())
            val hasPro = allPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK ||
                inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK
            ) {
                saveProStatusToDataStore(hasPro)
                handlePurchases(allPurchases)
            }
        } catch (_: Exception) {
            // In offline zero-network mode, keep local DataStore value
        }
    }

    override suspend fun restorePurchases(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) {
                return@withContext Result.failure(Exception("No se pudo conectar con el servicio local de Google Play Store"))
            }

            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            val subsResult = billingClient.queryPurchasesAsync(subsParams)
            val inAppResult = billingClient.queryPurchasesAsync(inAppParams)

            val allPurchases = (subsResult.purchasesList.orEmpty() + inAppResult.purchasesList.orEmpty())
            val hasPro = allPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (hasPro) {
                handlePurchases(allPurchases)
                saveProStatusToDataStore(true)
                Result.success(true)
            } else {
                saveProStatusToDataStore(false)
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun launchBillingFlow(activity: Activity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) {
                return@withContext Result.failure(Exception("Servicio de Google Play Store no disponible"))
            }

            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_PRO)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_PRO)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = billingClient.queryProductDetails(params)
            val productDetails: ProductDetails? = productDetailsResult.productDetailsList?.firstOrNull()

            if (productDetails == null) {
                return@withContext Result.failure(
                    Exception("Producto Pro no encontrado en Google Play Store (ID: $PRODUCT_ID_PRO)")
                )
            }

            val productDetailsParamsList = when (productDetails.productType) {
                BillingClient.ProductType.SUBS -> {
                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken.orEmpty()
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                }
                else -> {
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                }
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            withContext(Dispatchers.Main) {
                val result = billingClient.launchBillingFlow(activity, billingFlowParams)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Result.success(Unit)
                } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    saveProStatusToDataStore(true)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(result.debugMessage.ifBlank { "Error al iniciar compra en Google Play (${result.responseCode})" }))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setDebugProStatus(isPro: Boolean) {
        repositoryScope.launch {
            saveProStatusToDataStore(isPro)
        }
    }
}
