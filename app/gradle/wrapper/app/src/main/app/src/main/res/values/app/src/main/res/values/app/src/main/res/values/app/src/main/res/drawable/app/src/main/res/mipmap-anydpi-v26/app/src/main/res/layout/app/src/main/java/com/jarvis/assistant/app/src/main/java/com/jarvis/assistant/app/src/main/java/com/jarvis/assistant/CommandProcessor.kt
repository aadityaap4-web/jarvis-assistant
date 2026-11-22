package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object CommandProcessor {

    fun process(context: Context, rawInput: String): String {
        val input = rawInput.trim()
        if (input.isEmpty()) return "Didn't catch a command."
        val lower = input.lowercase()

        return try {
            when {
                lower.startsWith("open ") && lower.contains(" and search ") ->
                    openBrowserAndSearch(context, input)

                lower.startsWith("open ") ->
                    openApp(context, input.removePrefix("open ").removePrefix("Open ").trim())

                lower.startsWith("call ") ->
                    callContact(context, input.removePrefix("call ").trim())

                (lower.startsWith("whatsapp ") || lower.contains("send whatsapp")) ->
                    sendWhatsApp(context, input)

                lower.startsWith("message ") || lower.startsWith("text ") ->
                    sendSms(context, input)

                lower.startsWith("search ") || lower.startsWith("google ") ->
                    webSearch(context, stripSearchVerb(input), targetPackage = null)

                else -> "I don't know that command yet. Try: \"open <app>\", \"call <name>\", " +
                        "\"whatsapp <name> saying <text>\", \"message <name> saying <text>\", or \"search <query>\"."
            }
        } catch (e: Exception) {
            "Something went wrong: ${e.message}"
        }
    }

    private fun openApp(context: Context, appName: String): String {
        if (appName.isBlank()) return "Which app should I open?"
        val intent = AppLauncher.findLaunchIntent(context, appName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening $appName."
        } else {
            "Couldn't find an app called \"$appName\" on this phone."
        }
    }

    private fun openBrowserAndSearch(context: Context, input: String): String {
        val afterOpen = input.substringAfter(' ', "").trim()
        val browserPart = afterOpen.substringBefore(" and search ").trim()
        val query = afterOpen.substringAfter(" and search ").trim()
        if (query.isBlank()) return "What should I search for?"

        val pkg = when (browserPart.lowercase()) {
            "chrome" -> "com.android.chrome"
            "opera" -> "com.opera.browser"
            "firefox" -> "org.mozilla.firefox"
            else -> null
        }
        return webSearch(context, query, pkg, browserLabel = browserPart)
    }

    private fun stripSearchVerb(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.startsWith("search ") -> input.substring(7)
            lower.startsWith("google ") -> input.substring(7)
            else -> input
        }.trim()
    }

    private fun webSearch(
        context: Context,
        query: String,
        targetPackage: String?,
        browserLabel: String = "your browser"
    ): String {
        if (query.isBlank()) return "What should I search for?"
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            targetPackage?.let { setPackage(it) }
        }
        return try {
            context.startActivity(intent)
            if (targetPackage != null) "Searching for \"$query\" in $browserLabel."
            else "Searching for \"$query\"."
        } catch (e: Exception) {
            intent.setPackage(null)
            context.startActivity(intent)
            "Couldn't find $browserLabel, searching for \"$query\" in your default browser instead."
        }
    }

    private fun callContact(context: Context, name: String): String {
        if (name.isBlank()) return "Who should I call?"
        val number = ContactResolver.findPhoneNumber(context, name)
            ?: return "No contact found matching \"$name\"."

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return if (hasCallPermission) "Calling $name ($number)."
        else "Opening dialer for $name ($number) — grant Call permission for Jarvis to dial automatically."
    }

    private fun sendSms(context: Context, input: String): String {
        val withoutVerb = input.substringAfter(' ').trim()
        val (name, body) = splitNameAndMessage(withoutVerb)
        if (name.isBlank()) return "Who should I message?"
        val number = ContactResolver.findPhoneNumber(context, name)
            ?: return "No contact found matching \"$name\"."

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opened a message to $name pre-filled — tap send to deliver it."
    }

    private fun sendWhatsApp(context: Context, input: String): String {
        val withoutVerb = input
            .replaceFirst(Regex("(?i)send whatsapp (to )?"), "")
            .replaceFirst(Regex("(?i)^whatsapp "), "")
            .trim()
        val (name, body) = splitNameAndMessage(withoutVerb)
        if (name.isBlank()) return "Who should I WhatsApp?"
        val number = ContactResolver.findPhoneNumber(context, name)
            ?: return "No contact found matching \"$name\"."

        val cleaned = number.replace("+", "").replace(" ", "").replace("-", "")
        val uri = Uri.parse("https://wa.me/$cleaned?text=" + Uri.encode(body))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opened WhatsApp chat with $name, message pre-filled — tap send to deliver it."
    }

    private fun splitNameAndMessage(text: String): Pair<String, String> {
        val markers = listOf(" saying ", " that ", " : ", ": ")
        for (marker in markers) {
            if (text.contains(marker, ignoreCase = true)) {
                val idx = text.indexOf(marker, ignoreCase = true)
                val name = text.substring(0, idx).trim()
                val body = text.substring(idx + marker.length).trim()
                return name to body
            }
        }
        val parts = text.split(" ", limit = 2)
        val name = parts.getOrElse(0) { "" }
        val body = parts.getOrElse(1) { "" }
        return name to body
    }
}
