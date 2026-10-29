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

        // geometry
        val insetX = 1.2f * d
        val insetY = 1.6f * d
        val l = insetX
        val t = insetY
        val r = w - insetX
        val b = h - insetY
        if (r - l < 4 || b - t < 4) return
        val cut = (min(r - l, b - t) * (if (isAction) 0.26f else 0.17f)).coerceIn(2.5f * d, 9f * d)
        path.reset()
        path.moveTo(l + cut, t)
        if (isAction) {
            // enter: cut top-left and top-right, more aggressive silhouette
            path.lineTo(r - cut * 0.6f, t)
            path.lineTo(r, t + cut * 0.6f)
        } else {
            path.lineTo(r, t)
        }
        path.lineTo(r, b - cut)
        path.lineTo(r - cut, b)
        path.lineTo(l, b)
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
                pressed -> ColorUtils.blendARGB(if (transparentBase) p.keySecondary else baseFill, pressAccent, 0.16f + 0.06f * fx)
                pulse > 0f -> ColorUtils.blendARGB(baseFill, pressAccent, 0.14f * pulse)
                else -> baseFill
            }
            canvas.drawPath(path, fill)
        }

        // glow under the border when active
        val glowStrength = when {
            pressed -> 1f
            pulse > 0f -> pulse
            else -> 0f
        } * fx * p.glow
        if (glowStrength > 0.01f && !transparentBase) {
            val glowColor = if (isAction) p.accentBright else pressAccent
            stroke.color = ColorUtils.setAlphaComponent(glowColor, (70 * glowStrength).toInt().coerceIn(0, 255))
            stroke.strokeWidth = 4.5f * d
            canvas.drawPath(path, stroke)
            stroke.color = ColorUtils.setAlphaComponent(glowColor, (45 * glowStrength).toInt().coerceIn(0, 255))
            stroke.strokeWidth = 6.5f * d
            canvas.drawPath(path, stroke)
        }

        // border
        val borderColor = when {
            isAction -> if (pressed) p.accentBright else ColorUtils.blendARGB(p.accent, p.accentBright, 0.4f)
            pressed -> pressAccent
            shiftState > 0 -> p.accent
            pulse > 0f -> ColorUtils.blendARGB(p.border, pressAccent, pulse)
            else -> p.border
        }
        val drawBorder = colors.hasKeyBorders || pressed || shiftState > 0 || pulse > 0f || isAction
        if (drawBorder && (!transparentBase || pressed)) {
            stroke.color = borderColor
            stroke.strokeWidth = (if (pressed || shiftState > 0) 1.4f else 1f) * d
            canvas.drawPath(path, stroke)
        }

        if (variant != Variant.MAIN) return

        // subtle top highlight for depth (not on accent key)
        if (!isAction && colors.hasKeyBorders) {
            line.color = 0x14FFFFFF
            line.strokeWidth = 1f * d
            canvas.drawLine(l + cut + 1.5f * d, t + 1.5f * d, r - 2f * d, t + 1.5f * d, line)
        }

        // caps lock indicator: solid bar at the bottom
        if (shiftState == 2) {
            block.color = p.accent
            val bw = (r - l) * 0.34f
            val cx = (l + r) / 2f
            canvas.drawRect(cx - bw / 2f, b - 3.2f * d, cx + bw / 2f, b - 1.4f * d, block)
        }

        if (!colors.techDecor || fx <= 0f) return
        when {
            isAction -> {
                // three hazard stripes on the lower-left
                line.color = ColorUtils.setAlphaComponent(p.onAccent, 0x66)
                line.strokeWidth = 1.4f * d
                val s = 4f * d
                for (i in 0..2) {
                    val x0 = l + 4f * d + i * s
                    canvas.drawLine(x0, b - 3f * d, x0 + s * 0.7f, b - 3f * d - s * 0.9f, line)
                }
            }
            isFunctional -> {
                // teal corner tick at the top-right
                line.color = ColorUtils.setAlphaComponent(p.teal, if (p.technical) 0xE0 else 0xA0)
                line.strokeWidth = 1.3f * d
                val len = min(6f * d, (r - l) * 0.2f)
                canvas.drawLine(r - len - 2f * d, t + 2.2f * d, r - 2.2f * d, t + 2.2f * d, line)
                canvas.drawLine(r - 2.2f * d, t + 2.2f * d, r - 2.2f * d, t + len + 2f * d, line)
            }
            isSpace -> {
                // centered yellow track with teal end markers
                val cx = (l + r) / 2f
                val tw = max(24f * d, (r - l) * 0.22f)
                line.color = ColorUtils.setAlphaComponent(p.accent, 0xB0)
                line.strokeWidth = 1.6f * d
                canvas.drawLine(cx - tw / 2f, b - 3.5f * d, cx + tw / 2f, b - 3.5f * d, line)
                block.color = ColorUtils.setAlphaComponent(p.teal, 0xC0)
                canvas.drawRect(l + 5f * d, b - 5f * d, l + 8f * d, b - 2.5f * d, block)
                canvas.drawRect(r - 8f * d - cut, b - 5f * d, r - 5f * d - cut, b - 2.5f * d, block)
            }
            p.technical -> {
                // netrunner: tiny technical line on the bottom-left of letter keys
                line.color = ColorUtils.setAlphaComponent(p.teal, 0x50)
                line.strokeWidth = 1f * d
                canvas.drawLine(l + 2.5f * d, b - 2.5f * d, l + 2.5f * d + min(7f * d, (r - l) * 0.25f), b - 2.5f * d, line)
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
