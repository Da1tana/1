// SPDX-License-Identifier: GPL-3.0-only
// Night City Keyboard — cyberpunk visual system on top of HeliBoard's Colors interface.
package helium314.keyboard.latin.common

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.common.ColorType.*
import helium314.keyboard.latin.suggestions.SuggestionStripView
import kotlin.math.min

/**
 * One of the three Night City visual profiles.
 * All colors are ARGB ints. [id] is the value stored in the theme colors preference.
 */
class NightCityPalette(
    val id: String,
    /** keyboard panel background (top of gradient) */
    val panel: Int,
    /** deepest background (bottom of gradient, navigation bar) */
    val base: Int,
    /** letter key surface */
    val key: Int,
    /** functional key surface (shift, backspace, symbols, ...) */
    val keySecondary: Int,
    /** space bar surface */
    val spaceBar: Int,
    /** main accent (yellow) — enter key, active states */
    val accent: Int,
    /** brighter accent — pressed enter, highlights */
    val accentBright: Int,
    /** secondary accent (teal) — indicators, functional key accents */
    val teal: Int,
    /** letter color */
    val keyText: Int,
    /** functional key icons / labels */
    val functionalText: Int,
    /** hint letters, secondary text */
    val hintText: Int,
    /** thin key borders */
    val border: Int,
    /** text/icon on bright accent surfaces */
    val onAccent: Int,
    /** pressed accent for letters (yellow or teal, depends on profile) */
    val pressAccent: Int,
    /** pressed accent for functional keys */
    val pressAccentFunctional: Int,
    /** 0..1 multiplier for glow strength of this profile */
    val glow: Float,
    /** whether technical lines/brackets are drawn more prominently */
    val technical: Boolean,
) {
    companion object {
        const val NIGHT_CITY = "night_city"
        const val AFTERLIFE = "afterlife"
        const val NETRUNNER = "netrunner"

        val NIGHT_CITY_PALETTE = NightCityPalette(
            id = NIGHT_CITY,
            panel = 0xFF0D1116.toInt(),
            base = 0xFF080B0E.toInt(),
            key = 0xFF171D23.toInt(),
            keySecondary = 0xFF202830.toInt(),
            spaceBar = 0xFF171D23.toInt(),
            accent = 0xFFF5C542.toInt(),
            accentBright = 0xFFFFE36E.toInt(),
            teal = 0xFF35E6D0.toInt(),
            keyText = 0xFFF5C542.toInt(),
            functionalText = 0xFFE9EEF2.toInt(),
            hintText = 0xFFA3ADB6.toInt(),
            border = 0xFF34414A.toInt(),
            onAccent = 0xFF080B0E.toInt(),
            pressAccent = 0xFFF5C542.toInt(),
            pressAccentFunctional = 0xFF35E6D0.toInt(),
            glow = 0.8f,
            technical = false,
        )

        val AFTERLIFE_PALETTE = NightCityPalette(
            id = AFTERLIFE,
            panel = 0xFF07090B.toInt(),
            base = 0xFF020304.toInt(),
            key = 0xFF111519.toInt(),
            keySecondary = 0xFF171C21.toInt(),
            spaceBar = 0xFF111519.toInt(),
            accent = 0xFFD9B24A.toInt(),
            accentBright = 0xFFEACB6B.toInt(),
            teal = 0xFF2FC9B6.toInt(),
            keyText = 0xFFE6DFC8.toInt(),
            functionalText = 0xFFD9B24A.toInt(),
            hintText = 0xFF7E8892.toInt(),
            border = 0xFF1F272D.toInt(),
            onAccent = 0xFF050607.toInt(),
            pressAccent = 0xFFD9B24A.toInt(),
            pressAccentFunctional = 0xFF2FC9B6.toInt(),
            glow = 0.45f,
            technical = false,
        )

        val NETRUNNER_PALETTE = NightCityPalette(
            id = NETRUNNER,
            panel = 0xFF071116.toInt(),
            base = 0xFF04080B.toInt(),
            key = 0xFF0E1A20.toInt(),
            keySecondary = 0xFF13252D.toInt(),
            spaceBar = 0xFF0E1A20.toInt(),
            accent = 0xFFF5C542.toInt(),
            accentBright = 0xFFFFE36E.toInt(),
            teal = 0xFF35E6D0.toInt(),
            keyText = 0xFF8FF5E8.toInt(),
            functionalText = 0xFF35E6D0.toInt(),
            hintText = 0xFF5FA9A1.toInt(),
            border = 0xFF1D4A4A.toInt(),
            onAccent = 0xFF061014.toInt(),
            pressAccent = 0xFF35E6D0.toInt(),
            pressAccentFunctional = 0xFFF5C542.toInt(),
            glow = 1f,
            technical = true,
        )

        val ALL = listOf(NIGHT_CITY_PALETTE, AFTERLIFE_PALETTE, NETRUNNER_PALETTE)

        fun forId(id: String?): NightCityPalette? = ALL.firstOrNull { it.id == id }
        fun isNightCityTheme(id: String?) = forId(id) != null
    }
}

/** Visual effect level chosen by the user. */
object NightCityFxLevel {
    const val MINIMAL = "minimal"
    const val STANDARD = "standard"
    const val MAX = "max"

    fun factor(level: String?) = when (level) {
        MINIMAL -> 0f
        MAX -> 1.35f
        else -> 1f
    }
}

/**
 * Colors implementation for the Night City themes. Most of the state handling is delegated to
 * [DefaultColors] (so all existing views keep working), and the cyberpunk specific parts
 * (accent action key, panel drawables, strip decoration) are overridden here.
 * The keys themselves are drawn by [helium314.keyboard.keyboard.CyberKeyRenderer].
 */
class NightCityColors(
    val palette: NightCityPalette,
    themeStyle: String,
    hasKeyBorders: Boolean,
    private val keyboardBackgroundImage: Drawable?,
    /** 0 = minimal ... 1.35 = max */
    val fxFactor: Float,
    val techDecor: Boolean,
    val suggestionFrame: Boolean,
    val animations: Boolean,
) : Colors {
    private val base = DefaultColors(
        themeStyle,
        hasKeyBorders,
        palette.accent,
        palette.panel,
        palette.key,
        palette.keySecondary,
        palette.spaceBar,
        palette.keyText,
        palette.hintText,
        suggestionText = palette.functionalText,
        spaceBarText = ColorUtils.setAlphaComponent(palette.hintText, 0xB0),
        gesture = palette.teal,
        keyboardBackground = keyboardBackgroundImage,
    )

    override val themeStyle: String get() = base.themeStyle
    override val hasKeyBorders: Boolean get() = base.hasKeyBorders

    override fun get(color: ColorType): Int = when (color) {
        ACTION_KEY_ICON -> palette.onAccent
        ACTION_KEY_BACKGROUND, ACTION_KEY_POPUP_KEYS_BACKGROUND -> palette.accent
        SHIFT_KEY_ICON -> palette.accent
        KEY_ICON, FUNCTIONAL_KEY_TEXT -> palette.functionalText
        KEY_TEXT, POPUP_KEY_TEXT, KEY_PREVIEW_TEXT -> palette.keyText
        POPUP_KEY_ICON -> palette.functionalText
        KEY_HINT_TEXT -> palette.hintText
        GESTURE_TRAIL -> palette.teal
        GESTURE_PREVIEW, POPUP_KEYS_BACKGROUND, KEY_PREVIEW_BACKGROUND, MORE_SUGGESTIONS_BACKGROUND -> palette.keySecondary
        SUGGESTION_AUTO_CORRECT -> palette.accent
        SUGGESTION_TYPED_WORD -> palette.teal
        SUGGESTED_WORD, SUGGESTION_VALID_WORD, MORE_SUGGESTIONS_HINT -> palette.functionalText
        EMOJI_CATEGORY_SELECTED, CLIPBOARD_PIN, TOOL_BAR_KEY_ENABLED_BACKGROUND -> palette.accent
        MAIN_BACKGROUND, STRIP_BACKGROUND, MORE_SUGGESTIONS_WORD_BACKGROUND -> palette.panel
        NAVIGATION_BAR -> palette.base
        else -> base.get(color)
    }

    override fun haveColorsChanged(context: android.content.Context) = false

    override fun setColor(drawable: Drawable, color: ColorType) {
        when (color) {
            ACTION_KEY_ICON, SHIFT_KEY_ICON, KEY_ICON, POPUP_KEY_ICON, EMOJI_CATEGORY_SELECTED, CLIPBOARD_PIN ->
                drawable.colorFilter = filter(get(color))
            else -> base.setColor(drawable, color)
        }
    }

    override fun setColor(view: ImageView, color: ColorType) {
        when (color) {
            EMOJI_CATEGORY_SELECTED, CLIPBOARD_PIN, KEY_ICON, ACTION_KEY_ICON -> view.colorFilter = filter(get(color))
            else -> base.setColor(view, color)
        }
    }

    override fun setBackground(view: View, color: ColorType) {
        val res = view.resources
        when (color) {
            KEY_PREVIEW_BACKGROUND -> view.background = CyberPanelDrawable(res, palette, palette.keySecondary, palette.accent, fxFactor, accentEdge = true)
            POPUP_KEYS_BACKGROUND, MORE_SUGGESTIONS_BACKGROUND ->
                view.background = CyberPanelDrawable(res, palette, palette.keySecondary, palette.border, fxFactor, accentEdge = true)
            MAIN_BACKGROUND -> {
                if (keyboardBackgroundImage != null) base.setBackground(view, color)
                else view.background = CyberBackdropDrawable(res, palette, techDecor && fxFactor > 0f)
            }
            STRIP_BACKGROUND -> {
                if (view is SuggestionStripView && keyboardBackgroundImage == null)
                    view.background = CyberStripDrawable(res, palette, suggestionFrame, techDecor && fxFactor > 0f)
                else base.setBackground(view, color)
            }
            else -> base.setBackground(view, color)
        }
    }

    private val filters = HashMap<Int, ColorFilter>()
    private fun filter(color: Int) = filters.getOrPut(color) {
        androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color, androidx.core.graphics.BlendModeCompat.SRC_IN)!!
    }
}

/** Cut-corner panel with a thin border, used for key preview, popup keys and more suggestions. */
class CyberPanelDrawable(
    res: Resources,
    private val palette: NightCityPalette,
    private val fill: Int,
    private val stroke: Int,
    private val fxFactor: Float,
    private val accentEdge: Boolean,
) : Drawable() {
    private val density = res.displayMetrics.density
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = fill }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.2f * density; color = stroke
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f * density; color = palette.teal; strokeCap = Paint.Cap.SQUARE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 4f * density
        color = ColorUtils.setAlphaComponent(stroke, (40 * fxFactor * palette.glow).toInt().coerceIn(0, 255))
    }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val inset = 1f * density
        val l = b.left + inset; val t = b.top + inset; val r = b.right - inset; val btm = b.bottom - inset
        val cut = min(10f * density, min(r - l, btm - t) * 0.22f)
        path.reset()
        path.moveTo(l + cut, t)
        path.lineTo(r, t)
        path.lineTo(r, btm - cut)
        path.lineTo(r - cut, btm)
        path.lineTo(l, btm)
        path.lineTo(l, t + cut)
        path.close()
        canvas.drawPath(path, fillPaint)
        if (fxFactor > 0f) canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, strokePaint)
        if (accentEdge) {
            // short teal bracket on the top-left cut
            val len = min(14f * density, (r - l) * 0.3f)
            canvas.drawLine(l + cut + 2 * density, t + 1.5f * density, l + cut + len, t + 1.5f * density, accentPaint)
        }
    }

    override fun setAlpha(alpha: Int) { fillPaint.alpha = alpha; strokePaint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getPadding(padding: Rect): Boolean = false
}

/** Keyboard backdrop: vertical gradient with a thin accent line on top and optional grid ticks. */
class CyberBackdropDrawable(res: Resources, private val palette: NightCityPalette, private val decor: Boolean) : Drawable() {
    private val density = res.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f * density
    }
    private var shaderHeight = -1

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        if (shaderHeight != b.height()) {
            paint.shader = LinearGradient(0f, b.top.toFloat(), 0f, b.bottom.toFloat(),
                palette.panel, palette.base, Shader.TileMode.CLAMP)
            shaderHeight = b.height()
        }
        canvas.drawRect(b, paint)
        // top hairline
        linePaint.color = ColorUtils.setAlphaComponent(palette.border, 0xFF)
        canvas.drawLine(b.left.toFloat(), b.top + 0.5f * density, b.right.toFloat(), b.top + 0.5f * density, linePaint)
        if (decor) {
            // accent segment at the top-left, teal tick at the top-right
            linePaint.color = palette.accent
            linePaint.strokeWidth = 2f * density
            val segment = b.width() * 0.18f
            canvas.drawLine(b.left.toFloat(), b.top + 1f * density, b.left + segment, b.top + 1f * density, linePaint)
            linePaint.color = palette.teal
            canvas.drawLine(b.right - 28f * density, b.top + 1f * density, b.right - 12f * density, b.top + 1f * density, linePaint)
            linePaint.strokeWidth = 1f * density
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.OPAQUE
}

/** Suggestion strip background: panel color with bottom hairline and small terminal-like markers. */
class CyberStripDrawable(
    res: Resources,
    private val palette: NightCityPalette,
    private val frame: Boolean,
    private val decor: Boolean,
) : Drawable() {
    private val density = res.displayMetrics.density
    private val paint = Paint().apply { color = palette.panel }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f * density; color = palette.border }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        canvas.drawRect(b, paint)
        if (!frame) return
        val y = b.bottom - 0.5f * density
        canvas.drawLine(b.left.toFloat(), y, b.right.toFloat(), y, linePaint)
        if (decor) {
            // small yellow block left, teal block right: "status indicators"
            markPaint.color = palette.accent
            canvas.drawRect(b.left.toFloat(), b.bottom - 2.5f * density, b.left + 18f * density, b.bottom.toFloat(), markPaint)
            markPaint.color = palette.teal
            canvas.drawRect(b.right - 10f * density, b.bottom - 2.5f * density, b.right - 4f * density, b.bottom.toFloat(), markPaint)
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.OPAQUE
}
