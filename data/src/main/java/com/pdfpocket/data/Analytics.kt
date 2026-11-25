package com.pdfpocket.data

import android.util.Log

object Analytics {
    const val TOOL_USED = "tool_used"
    const val EXPORT_SUCCESS = "export_success"
    const val INTERSTITIAL_SHOWN = "interstitial_shown"
    const val REWARDED_COMPLETED = "rewarded_completed"
    const val IAP_REMOVE_ADS = "iap_remove_ads"

    fun log(event: String, tool: String? = null, success: Boolean = true) {
        Log.d("PDFPocketAnalytics", "$event tool=$tool success=$success")
    }
}
