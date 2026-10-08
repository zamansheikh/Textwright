package com.silifton.textwright.sms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import com.silifton.textwright.data.Sim

object Sims {

    /** Active SIMs (physical and eSIM) ordered by slot. Empty until READ_PHONE_STATE is granted. */
    @SuppressLint("MissingPermission")
    fun active(context: Context): List<Sim> {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        val infos = try {
            manager.activeSubscriptionInfoList
        } catch (e: SecurityException) {
            null
        }
        return infos.orEmpty()
            .sortedBy { it.simSlotIndex }
            .map { Sim(it.subscriptionId, it.simSlotIndex, it.displayName?.toString().orEmpty()) }
    }

    /** [preferred] if that SIM is active, else the system's default SMS SIM, else the first one. */
    fun choose(sims: List<Sim>, preferred: Int? = null): Int {
        if (preferred != null && sims.any { it.subId == preferred }) return preferred
        val default = SubscriptionManager.getDefaultSmsSubscriptionId()
        if (sims.any { it.subId == default }) return default
        return sims.firstOrNull()?.subId ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }
}
