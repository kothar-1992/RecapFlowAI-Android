package com.recapflow.ai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.recapflow.ai.R

/**
 * Self-contained Settings card for RecapFlowAI's two supported app languages.
 *
 * AppCompat owns locale persistence through AppLocalesMetadataHolderService on Android 12 and
 * lower, while Android 13+ uses the platform app-locale store. Changing the locale recreates the
 * Activity; MainActivity's normal instance-state/editor recovery path keeps the active project.
 */
class LanguageSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val languageGroup: MaterialButtonToggleGroup
    private var rendering = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_language_settings, this, true)
        languageGroup = findViewById(R.id.settingsLanguageGroup)
        renderSelection()
        languageGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || rendering) return@addOnButtonCheckedListener
            val languageTag = when (checkedId) {
                R.id.settingsLanguageMyanmarButton -> LANGUAGE_MYANMAR
                else -> LANGUAGE_ENGLISH
            }
            val currentLanguage = resources.configuration.locales.get(0)?.language.orEmpty()
            if (currentLanguage == languageTag) return@addOnButtonCheckedListener
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag),
            )
        }
    }

    private fun renderSelection() {
        val currentLanguage = resources.configuration.locales.get(0)?.language.orEmpty()
        rendering = true
        languageGroup.check(
            if (currentLanguage == LANGUAGE_MYANMAR) {
                R.id.settingsLanguageMyanmarButton
            } else {
                R.id.settingsLanguageEnglishButton
            },
        )
        rendering = false
    }

    companion object {
        private const val LANGUAGE_ENGLISH = "en"
        private const val LANGUAGE_MYANMAR = "my"
    }
}
