package com.recapflow.ai.ui

import java.net.URI

/** Version name and code rendered in the side menu, with the code already ASCII-safe. */
data class SideMenuVersion(val name: String, val code: String)

/**
 * Pure decision logic behind the Phase 6UX.2 side menu.
 *
 * Deliberately free of Android framework types so the HTTPS gate and the version fallback rules
 * stay unit-testable on the JVM, matching the project's other `*Policy` helpers. The controller
 * keeps all framework work (intents, dialogs, the toolbar) and delegates every decision here.
 */
object SideMenuPolicy {

    /**
     * Community destinations are opened with `ACTION_VIEW`, so only an absolute HTTPS link with a
     * real host may leave the app. Everything else is treated as unconfigured rather than
     * attempted, which keeps a bad resource value from reaching an external handler.
     */
    fun isHttpsDestination(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    /** A blank developer address cannot build a `mailto:` intent, so it is reported as unconfigured. */
    fun isContactableEmail(raw: String?): Boolean = !raw?.trim().isNullOrEmpty()

    /**
     * Prefers the installed package metadata and falls back to BuildConfig, matching how the
     * version is always shown even when `PackageManager` refuses to answer.
     *
     * The code is converted with `Long.toString()` so it can never pick up Myanmar numerals from
     * a locale-aware formatter in a localized UI.
     */
    fun resolveVersion(
        packageVersionName: String?,
        packageVersionCode: Long?,
        fallbackVersionName: String,
        fallbackVersionCode: Long,
    ): SideMenuVersion = SideMenuVersion(
        name = packageVersionName?.takeIf { it.isNotBlank() } ?: fallbackVersionName,
        code = (packageVersionCode ?: fallbackVersionCode).toString(),
    )
}
