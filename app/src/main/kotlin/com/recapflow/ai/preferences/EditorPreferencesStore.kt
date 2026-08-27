package com.recapflow.ai.preferences

import android.content.Context
import android.content.SharedPreferences
import com.recapflow.ai.media.edit.AdaptiveCutPreset
import com.recapflow.ai.media.edit.AspectRatioPreset
import com.recapflow.ai.media.edit.AudioPolicy
import com.recapflow.ai.media.edit.BlurRectangle
import com.recapflow.ai.media.edit.ColorSettings
import com.recapflow.ai.media.edit.CropRectangle
import com.recapflow.ai.media.edit.CropSettings
import com.recapflow.ai.media.edit.FreezeSettings
import com.recapflow.ai.media.edit.ScaleMode
import com.recapflow.ai.media.edit.TransformSettings
import com.recapflow.ai.media.edit.TransitionMode
import com.recapflow.ai.media.edit.TransitionSettings
import com.recapflow.ai.media.edit.ZoomMode
import com.recapflow.ai.media.edit.ZoomSettings
import com.recapflow.ai.media.render.RenderPreset

class EditorPreferencesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE,
    )

    var autoRestoreEnabled: Boolean
        get() = runCatching { preferences.getBoolean(KEY_AUTO_RESTORE, true) }
            .getOrDefault(true)
        set(value) {
            preferences.edit().putBoolean(KEY_AUTO_RESTORE, value).apply()
        }

    val hasPreset: Boolean
        get() = runCatching {
            preferences.getInt(key(PREFIX_PRESET, KEY_SCHEMA), 0) in
                MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION
        }.getOrDefault(false)

    fun saveLastSession(snapshot: EditorPreferencesSnapshot) {
        preferences.edit().putSnapshot(PREFIX_LAST, snapshot).apply()
    }

    fun loadLastSession(): EditorPreferencesSnapshot? = runCatching {
        readSnapshot(PREFIX_LAST)
    }.getOrNull()

    fun savePreset(snapshot: EditorPreferencesSnapshot) {
        preferences.edit()
            .putSnapshot(PREFIX_PRESET, snapshot)
            .putLong(KEY_PRESET_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun loadPreset(): EditorPreferencesSnapshot? = runCatching {
        readSnapshot(PREFIX_PRESET)
    }.getOrNull()

    fun clearSavedState() {
        preferences.edit().also { editor ->
            preferences.all.keys
                .filter { it.startsWith(PREFIX_LAST) || it.startsWith(PREFIX_PRESET) }
                .forEach(editor::remove)
            editor.remove(KEY_PRESET_SAVED_AT)
        }.apply()
    }

    private fun SharedPreferences.Editor.putSnapshot(
        prefix: String,
        rawSnapshot: EditorPreferencesSnapshot,
    ): SharedPreferences.Editor {
        val snapshot = EditorPreferencesPolicy.sanitize(rawSnapshot)
        val transform = snapshot.transform
        val audio = snapshot.audio
        val overlay = snapshot.overlay
        return putInt(key(prefix, KEY_SCHEMA), SCHEMA_VERSION)
            .putBoolean(key(prefix, "transform.enabled"), transform.enabled)
            .putString(key(prefix, "transform.aspect"), transform.aspectRatio.name)
            .putString(key(prefix, "transform.scale"), transform.scaleMode.name)
            .putBoolean(key(prefix, "transform.crop.enabled"), transform.crop.enabled)
            .putFloat(key(prefix, "transform.crop.left"), transform.crop.rectangle.left)
            .putFloat(key(prefix, "transform.crop.top"), transform.crop.rectangle.top)
            .putFloat(key(prefix, "transform.crop.right"), transform.crop.rectangle.right)
            .putFloat(key(prefix, "transform.crop.bottom"), transform.crop.rectangle.bottom)
            .putBoolean(key(prefix, "transform.mirror"), transform.mirrorEnabled)
            .putBoolean(key(prefix, "transform.color.enabled"), transform.color.enabled)
            .putFloat(key(prefix, "transform.color.brightness"), transform.color.brightness)
            .putFloat(key(prefix, "transform.color.contrast"), transform.color.contrast)
            .putFloat(key(prefix, "transform.color.saturation"), transform.color.saturation)
            .putFloat(key(prefix, "transform.color.temperature"), transform.color.temperature)
            .putBoolean(key(prefix, "transform.zoom.enabled"), transform.zoom.enabled)
            .putString(key(prefix, "transform.zoom.mode"), transform.zoom.mode.name)
            .putBoolean(key(prefix, "transform.speed.enabled"), transform.speedEnabled)
            .putFloat(key(prefix, "transform.speed.value"), transform.speed)
            .putBoolean(key(prefix, "transform.freeze.enabled"), transform.freeze.enabled)
            .putLong(key(prefix, "transform.freeze.duration"), transform.freeze.durationMs)
            .putBoolean(key(prefix, "transform.transition.enabled"), transform.transition.enabled)
            .putString(key(prefix, "transform.transition.mode"), transform.transition.mode.name)
            .putLong(
                key(prefix, "transform.transition.duration"),
                transform.transition.durationMs,
            )
            .putBoolean(key(prefix, "audio.enabled"), audio.enabled)
            .putString(key(prefix, "audio.policy"), audio.policy.name)
            .putFloat(key(prefix, "audio.volume"), audio.volume)
            .putFloat(key(prefix, "audio.mixSource"), audio.mixSourceVolume)
            .putFloat(key(prefix, "audio.mixAdded"), audio.mixAddedVolume)
            .putBoolean(key(prefix, "overlay.enabled"), overlay.enabled)
            .putBoolean(key(prefix, "overlay.blur.enabled"), overlay.blurEnabled)
            .putFloat(key(prefix, "overlay.blur.left"), overlay.blurRectangle.left)
            .putFloat(key(prefix, "overlay.blur.top"), overlay.blurRectangle.top)
            .putFloat(key(prefix, "overlay.blur.right"), overlay.blurRectangle.right)
            .putFloat(key(prefix, "overlay.blur.bottom"), overlay.blurRectangle.bottom)
            .putFloat(key(prefix, "overlay.blur.strength"), overlay.blurStrength)
            .putBoolean(key(prefix, "overlay.image.enabled"), overlay.imageEnabled)
            .putFloat(key(prefix, "overlay.image.centerX"), overlay.imageCenterX)
            .putFloat(key(prefix, "overlay.image.centerY"), overlay.imageCenterY)
            .putFloat(key(prefix, "overlay.image.width"), overlay.imageWidthFraction)
            .putFloat(key(prefix, "overlay.image.opacity"), overlay.imageOpacity)
            .putString(key(prefix, "adaptive.preset"), snapshot.adaptivePreset.name)
            .putString(key(prefix, "export.preset"), snapshot.renderPreset.name)
            .putString(key(prefix, "ui.section"), snapshot.selectedSection.name)
            .putBoolean(key(prefix, "ui.transformExpanded"), snapshot.transformDetailsVisible)
            .putBoolean(key(prefix, "ui.overlayExpanded"), snapshot.overlayDetailsVisible)
            .putFloat(key(prefix, "ui.previewScale"), snapshot.previewScale)
            .putFloat(key(prefix, "ui.previewCenterX"), snapshot.previewCenterX)
            .also { editor ->
                val centerYKey = key(prefix, "ui.previewCenterY")
                snapshot.previewCenterY?.let { editor.putFloat(centerYKey, it) }
                    ?: editor.remove(centerYKey)
            }
    }

    private fun readSnapshot(prefix: String): EditorPreferencesSnapshot? {
        if (
            preferences.getInt(key(prefix, KEY_SCHEMA), 0) !in
            MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION
        ) return null
        val defaults = EditorPreferencesSnapshot()
        val defaultTransform = defaults.transform
        val crop = CropRectangle(
            left = float(prefix, "transform.crop.left", defaultTransform.crop.rectangle.left),
            top = float(prefix, "transform.crop.top", defaultTransform.crop.rectangle.top),
            right = float(prefix, "transform.crop.right", defaultTransform.crop.rectangle.right),
            bottom = float(prefix, "transform.crop.bottom", defaultTransform.crop.rectangle.bottom),
        )
        val color = ColorSettings(
            enabled = bool(prefix, "transform.color.enabled", false),
            brightness = float(prefix, "transform.color.brightness", 0f),
            contrast = float(prefix, "transform.color.contrast", 0f),
            saturation = float(prefix, "transform.color.saturation", 0f),
            temperature = float(prefix, "transform.color.temperature", 0f),
        )
        val transform = TransformSettings(
            enabled = bool(prefix, "transform.enabled", false),
            aspectRatio = enumValue(prefix, "transform.aspect", AspectRatioPreset.ORIGINAL),
            scaleMode = enumValue(prefix, "transform.scale", ScaleMode.FIT),
            crop = CropSettings(bool(prefix, "transform.crop.enabled", false), crop),
            zoom = ZoomSettings(
                bool(prefix, "transform.zoom.enabled", false),
                enumValue(prefix, "transform.zoom.mode", ZoomMode.IN),
            ),
            mirrorEnabled = bool(prefix, "transform.mirror", false),
            color = color,
            freeze = FreezeSettings(
                bool(prefix, "transform.freeze.enabled", false),
                long(prefix, "transform.freeze.duration", defaultTransform.freeze.durationMs),
            ),
            speedEnabled = bool(prefix, "transform.speed.enabled", false),
            speed = float(prefix, "transform.speed.value", defaultTransform.speed),
            transition = TransitionSettings(
                bool(prefix, "transform.transition.enabled", false),
                enumValue(prefix, "transform.transition.mode", TransitionMode.FADE_IN_OUT),
                long(
                    prefix,
                    "transform.transition.duration",
                    defaultTransform.transition.durationMs,
                ),
            ),
        )
        val audio = AudioPreference(
            enabled = bool(prefix, "audio.enabled", false),
            policy = enumValue(prefix, "audio.policy", AudioPolicy.KEEP_ORIGINAL),
            volume = float(prefix, "audio.volume", defaults.audio.volume),
            mixSourceVolume = float(prefix, "audio.mixSource", defaults.audio.mixSourceVolume),
            mixAddedVolume = float(prefix, "audio.mixAdded", defaults.audio.mixAddedVolume),
        )
        val blur = BlurRectangle(
            left = float(prefix, "overlay.blur.left", defaults.overlay.blurRectangle.left),
            top = float(prefix, "overlay.blur.top", defaults.overlay.blurRectangle.top),
            right = float(prefix, "overlay.blur.right", defaults.overlay.blurRectangle.right),
            bottom = float(prefix, "overlay.blur.bottom", defaults.overlay.blurRectangle.bottom),
        )
        val overlay = OverlayPreference(
            enabled = bool(prefix, "overlay.enabled", false),
            blurEnabled = bool(prefix, "overlay.blur.enabled", false),
            blurRectangle = blur,
            blurStrength = float(prefix, "overlay.blur.strength", defaults.overlay.blurStrength),
            imageEnabled = bool(prefix, "overlay.image.enabled", false),
            imageCenterX = float(prefix, "overlay.image.centerX", defaults.overlay.imageCenterX),
            imageCenterY = float(prefix, "overlay.image.centerY", defaults.overlay.imageCenterY),
            imageWidthFraction = float(
                prefix,
                "overlay.image.width",
                defaults.overlay.imageWidthFraction,
            ),
            imageOpacity = float(prefix, "overlay.image.opacity", defaults.overlay.imageOpacity),
        )
        val centerYKey = key(prefix, "ui.previewCenterY")
        return EditorPreferencesPolicy.sanitize(
            EditorPreferencesSnapshot(
                transform = transform,
                audio = audio,
                overlay = overlay,
                adaptivePreset = enumValue(prefix, "adaptive.preset", defaults.adaptivePreset),
                renderPreset = enumValue(prefix, "export.preset", RenderPreset.DEFAULT),
                selectedSection = enumValue(prefix, "ui.section", defaults.selectedSection),
                transformDetailsVisible = bool(prefix, "ui.transformExpanded", true),
                overlayDetailsVisible = bool(prefix, "ui.overlayExpanded", true),
                previewScale = float(prefix, "ui.previewScale", defaults.previewScale),
                previewCenterX = float(prefix, "ui.previewCenterX", defaults.previewCenterX),
                previewCenterY = if (preferences.contains(centerYKey)) {
                    preferences.getFloat(centerYKey, 0.5f)
                } else {
                    null
                },
            ),
        )
    }

    private fun bool(prefix: String, name: String, default: Boolean): Boolean =
        preferences.getBoolean(key(prefix, name), default)

    private fun float(prefix: String, name: String, default: Float): Float =
        preferences.getFloat(key(prefix, name), default)

    private fun long(prefix: String, name: String, default: Long): Long =
        preferences.getLong(key(prefix, name), default)

    private inline fun <reified T : Enum<T>> enumValue(
        prefix: String,
        name: String,
        default: T,
    ): T = preferences.getString(key(prefix, name), null)
        ?.let { saved -> enumValues<T>().firstOrNull { it.name == saved } }
        ?: default

    companion object {
        const val SCHEMA_VERSION = 2
        private const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        private const val FILE_NAME = "recapflow_editor_preferences"
        private const val KEY_AUTO_RESTORE = "autoRestore"
        private const val KEY_PRESET_SAVED_AT = "preset.savedAt"
        private const val KEY_SCHEMA = "schema"
        private const val PREFIX_LAST = "last."
        private const val PREFIX_PRESET = "preset."

        private fun key(prefix: String, name: String): String = prefix + name
    }
}
