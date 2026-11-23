package com.example.billing

/**
 * Play Billing readiness abstraction for future optional digital upgrades.
 * TradeJournal v1.0.0 is 100% free and contains no third-party payment links (no UPI/Razorpay).
 * When premium features are introduced, wire this interface to Google Play Billing Library
 * (`com.android.billingclient:billing-ktx`) and add `com.android.vending.BILLING` only then.
 */
enum class SubscriptionTier {
    FREE
}

sealed interface EntitlementState {
    data object Free : EntitlementState
    data class Active(
        val tier: SubscriptionTier,
        val productId: String,
        val autoRenewing: Boolean,
        val inGracePeriod: Boolean = false
    ) : EntitlementState
    data class Error(val message: String) : EntitlementState
}

data class PaywallDisclosure(
    val title: String,
    val priceDisplay: String,
    val renewalTerms: String,
    val cancelInstructions: String
) {
    companion object {
        val template = PaywallDisclosure(
            title = "TradeJournal Optional Upgrade",
            priceDisplay = "Price shown in local currency via Google Play before purchase",
            renewalTerms = "Subscriptions renew automatically unless canceled at least 24 hours before the end of the current period.",
            cancelInstructions = "Manage or cancel anytime in Google Play Store > Profile > Payments & subscriptions > Subscriptions."
        )
    }
}

interface BillingEntitlementGateway {
    val currentState: EntitlementState
    suspend fun restorePurchases(): EntitlementState
    suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean
}

class FreeOnlyBillingGateway : BillingEntitlementGateway {
    override val currentState: EntitlementState = EntitlementState.Free

    override suspend fun restorePurchases(): EntitlementState = EntitlementState.Free

    override suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean = true
}
