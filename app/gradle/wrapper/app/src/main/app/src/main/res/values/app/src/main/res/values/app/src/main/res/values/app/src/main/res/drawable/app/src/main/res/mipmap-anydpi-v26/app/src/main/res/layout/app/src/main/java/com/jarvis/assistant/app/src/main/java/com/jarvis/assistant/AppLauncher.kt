package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppLauncher {

    private val aliases = mapOf(
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "opera" to "com.opera.browser",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "camera" to "com.android.camera",
        "settings" to "com.android.settings",
        "play store" to "com.android.vending",
        "telegram" to "org.telegram.messenger"
    )

    fun findLaunchIntent(context: Context, spokenName: String): Intent? {
        val pm = context.packageManager
        val key = spokenName.trim().lowercase()

        aliases[key]?.let { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let { return it }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

        var best: Pair<String, String>? = null
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val labelLower = label.lowercase()
            if (labelLower == key) {
                return pm.getLaunchIntentForPackage(info.activityInfo.packageName)
            }
            if (labelLower.contains(key) || key.contains(labelLower)) {
                if (best == null || labelLower.length < best!!.first.length) {
                    best = label to info.activityInfo.packageName
                }
            }
        }
        return best?.let { pm.getLaunchIntentForPackage(it.second) }
    }
}
