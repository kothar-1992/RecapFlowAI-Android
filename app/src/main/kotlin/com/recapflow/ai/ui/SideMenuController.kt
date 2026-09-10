package com.recapflow.ai.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.recapflow.ai.BuildConfig
import com.recapflow.ai.R

class SideMenuController(
    private val activity: AppCompatActivity,
    private val drawerLayout: DrawerLayout,
    private val toolbar: MaterialToolbar,
    private val navigationView: NavigationView,
) {
    private val menuDrawable = DrawerArrowDrawable(activity).apply {
        progress = 0f
        color = ContextCompat.getColor(activity, R.color.rf_on_surface)
    }

    fun bind() {
        toolbar.navigationIcon = menuDrawable
        toolbar.setNavigationContentDescription(R.string.drawer_open)
        toolbar.setNavigationOnClickListener { open() }
        renderHeader()
        navigationView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.drawerAccount -> {
                    Snackbar.make(
                        drawerLayout,
                        R.string.drawer_future_auth_note,
                        Snackbar.LENGTH_LONG,
                    ).show()
                    true
                }
                R.id.drawerContactDeveloper -> {
                    contactDeveloper()
                    true
                }
                R.id.drawerTelegram -> {
                    openHttpsDestination(R.string.side_menu_telegram_url)
                    true
                }
                R.id.drawerFacebook -> {
                    openHttpsDestination(R.string.side_menu_facebook_url)
                    true
                }
                R.id.drawerAppPolicy -> {
                    showLegalDialog(R.string.drawer_app_policy, R.string.drawer_app_policy_body)
                    true
                }
                R.id.drawerPrivacyPolicy -> {
                    showLegalDialog(R.string.drawer_privacy_policy, R.string.drawer_privacy_policy_body)
                    true
                }
                R.id.drawerTerms -> {
                    showLegalDialog(R.string.drawer_terms, R.string.drawer_terms_body)
                    true
                }
                R.id.drawerOpenSource -> {
                    showLegalDialog(R.string.drawer_open_source, R.string.drawer_open_source_body)
                    true
                }
                R.id.drawerAppVersion -> {
                    showVersionDialog()
                    true
                }
                else -> false
            }
            if (handled) close()
            handled
        }
    }

    fun open() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun close() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    fun closeIfOpen(): Boolean {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) return false
        close()
        return true
    }

    private fun renderHeader() {
        val header = navigationView.getHeaderView(0)
        header.findViewById<TextView>(R.id.drawerAccountName)
            .setText(R.string.drawer_account_guest)
        header.findViewById<TextView>(R.id.drawerUserLevel)
            .setText(R.string.drawer_user_level)
        header.findViewById<TextView>(R.id.drawerVersion).text = versionText()
    }

    private fun contactDeveloper() {
        val email = activity.getString(R.string.side_menu_developer_email).trim()
        if (email.isBlank()) {
            showUnavailable()
            return
        }
        val subject = activity.getString(R.string.drawer_contact_subject)
        val uri = Uri.parse(
            "mailto:${Uri.encode(email)}?subject=${Uri.encode(subject)}",
        )
        launchExternal(Intent(Intent.ACTION_SENDTO, uri))
    }

    private fun openHttpsDestination(@StringRes destinationRes: Int) {
        val uri = Uri.parse(activity.getString(destinationRes).trim())
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            showUnavailable()
            return
        }
        launchExternal(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchExternal(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showUnavailable()
        } catch (_: SecurityException) {
            showUnavailable()
        }
    }

    private fun showUnavailable() {
        Snackbar.make(
            drawerLayout,
            R.string.drawer_action_unavailable,
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun showLegalDialog(@StringRes titleRes: Int, @StringRes bodyRes: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(titleRes)
            .setMessage(bodyRes)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showVersionDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.drawer_app_version)
            .setMessage(versionText())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun versionText(): String {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getPackageInfo(
                    activity.packageName,
                    PackageManager.PackageInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getPackageInfo(activity.packageName, 0)
            }
        }.getOrNull()
        val versionName = info?.versionName
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.VERSION_NAME
        val versionCode = info?.let { PackageInfoCompat.getLongVersionCode(it) }
            ?: BuildConfig.VERSION_CODE.toLong()
        return activity.getString(
            R.string.drawer_version_format,
            versionName,
            versionCode.toString(),
        )
    }
}
