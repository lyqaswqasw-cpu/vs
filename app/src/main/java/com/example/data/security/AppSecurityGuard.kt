package com.example.data.security

import android.util.Log

/**
 * Enterprise-grade Anti-Defacement and Anti-Tamper Security Guard.
 * Protects the application, end-users, and Firebase synchronization against:
 * 1. Unauthorized server tampering, fake forced updates (e.g. 7.7.7).
 * 2. Malicious abusive, defamatory or profane text injection.
 * 3. Lockout traps that attempt to block users from accessing the app.
 */
object AppSecurityGuard {

    private const val TAG = "AppSecurityGuard"

    // Blacklist of defacement, profanity, and attack signatures
    private val FORBIDDEN_WORDS = listOf(
        // Defamation, severe profanity & insults (Arabic roots & common variations)
        "كحبة", "قحبة", "كحبه", "قحبه", "كحاب", "قحاب",
        "زب", "زبك", "زبنا", "كس", "كسك", "كسختك", "كسامك", "كس اختك", "كس امك",
        "منيوك", "منيوكه", "منيوكة", "تنايك", "نيج", "انيج", "ينيج",
        "طيز", "طيزك", "طيازة", "عرص", "عرصة", "عرصه",
        "ساقط", "ساقطة", "سافل", "سفلة", "حقير", "كلب", "ابن الكلب", "ابن الحرام", "ابن شرموطة",
        "شرموط", "شرموطة", "شرموطه", "شرايط",
        "تفو", "تفو عليك", "يلعن", "لعنة", "لعن", "العن", "يلعنك", "يلعن ابوك", "يلعن امك", "يلعن اهلك",
        "حمار", "يا حمار", "غبي", "حيوان", "ديوث", "قواد",

        // Attacker handles, taunts & hacking defacement terms
        "ابلع", "اختراق", "اخترق", "تم اختراقه", "تم الاختراق", "زعطوط", "علاوي", "norerror",
        "مواليد", "مهكر", "تهكير", "hack", "hacked", "hacker", "pwned", "exploit", "تخسرون", "حذف جميع بياناته",
        "مواليد 2008", "@norerror", "nor_error", "nor error", "طرد", "تم دعس", "تم الدعس",
        "باي باي", "هكر", "الهكر"
    )

    private val SUSPICIOUS_VERSION_PATTERNS = listOf(
        "7.7.7", "6.6.6", "9.9.9", "8.8.8", "666", "777", "999"
    )

    /**
     * Checks if the incoming text from server contains abusive, defamatory, or attack keywords.
     */
    fun isMaliciousText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase().trim()
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ة", "ه").replace("ى", "ي")
            .replace("_", " ").replace("-", " ")
        return FORBIDDEN_WORDS.any { keyword ->
            val normalizedKeyword = keyword.lowercase().trim()
                .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
                .replace("ة", "ه").replace("ى", "ي")
                .replace("_", " ").replace("-", " ")
            lower.contains(normalizedKeyword)
        }
    }

    /**
     * Sanitizes chat comments or video comments to completely strip abuse or replace with clean message.
     */
    fun sanitizeComment(text: String?): String {
        if (text.isNullOrBlank()) return ""
        if (isMaliciousText(text)) {
            return "تم حجب التعليق لمخالفته إرشادات الاستخدام 🛡️"
        }
        return text.trim()
    }

    /**
     * Validates whether a version number is reasonable or a spoofed/malicious version jump.
     * The app base version is 3.5.0. Legitimate updates must be strictly in the range 3.5.0 - 4.5.0.
     */
    fun isSuspiciousVersion(rawVersion: String?): Boolean {
        if (rawVersion.isNullOrBlank()) return false
        val cleaned = rawVersion.trim().removePrefix("v").removePrefix("V").trim()

        if (SUSPICIOUS_VERSION_PATTERNS.contains(cleaned)) {
            Log.w(TAG, "Blocked suspicious version pattern: $cleaned")
            return true
        }

        val parts = cleaned.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return true
        // If major version suddenly jumps to 5+, 7, or is 0, it is an attack/spoof
        if (major < 3 || major > 4) {
            Log.w(TAG, "Blocked illegitimate major version: $cleaned")
            return true
        }

        return false
    }

    /**
     * Sanitizes any update text, ensuring users NEVER see offensive, defacing, or hateful content.
     */
    fun sanitizeUpdateText(text: String?): String {
        if (text.isNullOrBlank() || isMaliciousText(text)) {
            return "يتوفر إصدار جديد من تطبيق FOUL لتحسين استقرار البث المباشر وتحديث القنوات والمكتبة."
        }
        return text.trim()
    }

    /**
     * Determines whether an update is genuine and legitimate.
     */
    fun isLegitimateUpdate(currentVersion: String, targetVersion: String, updateText: String): Boolean {
        if (isMaliciousText(updateText)) return false
        if (isSuspiciousVersion(targetVersion)) return false

        val currNorm = currentVersion.trim().removePrefix("v").removePrefix("V")
        val targNorm = targetVersion.trim().removePrefix("v").removePrefix("V")

        if (currNorm == targNorm) return false

        try {
            val currParts = currNorm.split(".").map { it.toIntOrNull() ?: 0 }
            val targParts = targNorm.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(currParts.size, targParts.size)
            for (i in 0 until maxLen) {
                val c = currParts.getOrElse(i) { 0 }
                val t = targParts.getOrElse(i) { 0 }
                if (c < t) return true
                if (c > t) return false
            }
        } catch (_: Exception) {}

        return false
    }
}
