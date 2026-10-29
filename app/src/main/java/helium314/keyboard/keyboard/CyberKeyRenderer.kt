// SPDX-License-Identifier: GPL-3.0-only
// Night City Keyboard — key renderer for the cyberpunk themes.
package helium314.keyboard.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.common.NightCityColors
import kotlin.math.max
import kotlin.math.min

/**
 * Draws key backgrounds as angular "cut corner" plates with thin borders, accent states and
 * a short release pulse. One instance per KeyboardView; all objects are preallocated so that
 * nothing is allocated while drawing.
 */
class CyberKeyRenderer(val colors: NightCityColors, density: Float) {
    private val d = density
    private val p = colors.palette
    private val fx = colors.fxFactor
    val animationsEnabled = colors.animations && fx > 0f

    private val path = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.MITER }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.SQUARE }
    private val block = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    enum class Variant { MAIN, POPUP, EMOJI }

    /**
     * @param shiftState 0 = not a shift key / off, 1 = shifted, 2 = caps lock
     * @param pulse remaining release-pulse strength 0..1
     */
    fun drawKey(canvas: Canvas, key: Key, w: Int, h: Int, pressed: Boolean, shiftState: Int, pulse: Float, variant: Variant) {
        if (w <= 0 || h <= 0) return
        val type = key.backgroundType
        if (type == Key.BACKGROUND_TYPE_EMPTY) return
        val isAction = type == Key.BACKGROUND_TYPE_ACTION
        val isFunctional = type == Key.BACKGROUND_TYPE_FUNCTIONAL
        val isSpace = type == Key.BACKGROUND_TYPE_SPACEBAR

        // geometry: airy keys, soft rounded corners and one small signature cut (top-left)
        val insetX = 2f * d
        val insetY = 2.8f * d
        val l = insetX
        val t = insetY
        val r = w - insetX
        val b = h - insetY
        if (r - l < 4 || b - t < 4) return
        val rad = min(7f * d, min(r - l, b - t) * 0.28f)
        val cut = min(4.5f * d, min(r - l, b - t) * 0.2f)
        path.reset()
        path.moveTo(l + cut, t)
        path.lineTo(r - rad, t)
        path.quadTo(r, t, r, t + rad)
        path.lineTo(r, b - rad)
        path.quadTo(r, b, r - rad, b)
        path.lineTo(l + rad, b)
        path.quadTo(l, b, l, b - rad)
        path.lineTo(l, t + cut)
        path.close()

        val transparentBase = variant != Variant.MAIN && !isAction
        val pressAccent = if (isFunctional || isSpace) p.pressAccentFunctional else p.pressAccent

        // fill
        val baseFill = when {
            isAction -> if (pressed) p.accentBright else p.accent
            isFunctional -> p.keySecondary
            isSpace -> p.spaceBar
            else -> p.key
        }
        if (!transparentBase || pressed) {
            fill.color = when {
                isAction -> baseFill
                pressed -> ColorUtils.blendARGB(
                    ColorUtils.blendARGB(if (transparentBase) p.key else baseFill, 0xFFFFFFFF.toInt(), 0.08f),
                    pressAccent, 0.10f)
                pulse > 0f -> ColorUtils.blendARGB(baseFill, 0xFFFFFFFF.toInt(), 0.07f * pulse)
                else -> baseFill
            }
            canvas.drawPath(path, fill)
        }

        // soft glow only at maximum intensity
        if (fx > 1f && (pressed || pulse > 0f) && !transparentBase) {
            val strength = (if (pressed) 1f else pulse) * p.glow
            stroke.color = ColorUtils.setAlphaComponent(if (isAction) p.accentBright else pressAccent, (45 * strength).toInt().coerceIn(0, 255))
            stroke.strokeWidth = 3f * d
            canvas.drawPath(path, stroke)
        }

        // borders: only when enabled by the user, or to show an active shift / press
        val borderColor = when {
            shiftState > 0 -> p.accent
            pressed -> ColorUtils.setAlphaComponent(pressAccent, 0xB0)
            else -> p.border
        }
        val drawBorder = !isAction && (colors.hasKeyBorders || shiftState > 0 || (pressed && fx > 0f))
        if (drawBorder && (!transparentBase || pressed)) {
            stroke.color = borderColor
            stroke.strokeWidth = (if (shiftState > 0) 1.3f else 0.8f) * d
            canvas.drawPath(path, stroke)
        }

        if (variant != Variant.MAIN) return

        // caps lock indicator: short rounded bar at the bottom
        if (shiftState == 2) {
            block.color = p.accent
            val bw = min(14f * d, (r - l) * 0.3f)
            val cx = (l + r) / 2f
            canvas.drawRoundRect(cx - bw / 2f, b - 4f * d, cx + bw / 2f, b - 2.4f * d, 1f * d, 1f * d, block)
        }

        if (!colors.techDecor || fx <= 0f) return
        // optional, deliberately quiet decorations
        when {
            isFunctional -> {
                block.color = ColorUtils.setAlphaComponent(p.teal, 0x90)
                canvas.drawCircle(r - 4.5f * d, t + 4.5f * d, 1.3f * d, block)
            }
            isSpace -> {
                val cx = (l + r) / 2f
                val tw = max(18f * d, (r - l) * 0.12f)
                line.color = ColorUtils.setAlphaComponent(p.accent, 0x70)
                line.strokeWidth = 1.4f * d
                line.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(cx - tw / 2f, b - 4f * d, cx + tw / 2f, b - 4f * d, line)
            }
        }
    }

    // ---- release pulse bookkeeping ---------------------------------------------------------

    private val pulseStarts = HashMap<Key, Long>()
    val pulseDurationMs = (150 + 60 * fx).toLong()

    fun startPulse(key: Key) {
        if (!animationsEnabled) return
        pulseStarts[key] = SystemClock.uptimeMillis()
    }

    fun clearPulses() = pulseStarts.clear()

    /** remaining strength 0..1 for the key, removes finished pulses */
    fun pulseFor(key: Key): Float {
        val start = pulseStarts[key] ?: return 0f
        val elapsed = SystemClock.uptimeMillis() - start
        if (elapsed >= pulseDurationMs) {
            pulseStarts.remove(key)
            return 0f
        }
        val x = 1f - elapsed.toFloat() / pulseDurationMs
        return x * x // ease-out
    }

    fun hasActivePulses() = pulseStarts.isNotEmpty()

    fun activePulseKeys(): Collection<Key> = pulseStarts.keys

    companion object {
        /** short fade-in for panels (popup keys, emoji, clipboard); no-op when animations are off */
        @JvmStatic
        fun fadeIn(view: View?, durationMs: Long) {
            if (view == null) return
            val colors = helium314.keyboard.latin.settings.Settings.getValues()?.mColors as? NightCityColors
            if (colors == null || !colors.animations || colors.fxFactor <= 0f) {
                view.alpha = 1f
                return
            }
            view.animate().cancel()
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(durationMs).start()
        }
    }
}
