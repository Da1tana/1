// SPDX-License-Identifier: GPL-3.0-only
// Night City Keyboard — home screen blocks: brand header, IME status, theme picker, preview.
package helium314.keyboard.settings

import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.NightCityFxLevel
import helium314.keyboard.latin.common.NightCityPalette
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.latin.utils.currentNightCityPalette
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs

private fun Int.c() = Color(this)

/** Brand header with icon, description and IME status / setup buttons. */
@Composable
fun NightCityHeader() {
    val ctx = LocalContext.current
    val p = currentNightCityPalette()
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // refresh status when returning from system settings / picker
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) { if (lifecycleState == Lifecycle.State.RESUMED) refresh++ }
    if (refresh < 0) Log.v("irrelevant", "recompose")
    val enabled = UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm)
    val current = UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(p.panel.c(), CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            .border(BorderStroke(1.dp, p.border.c()), CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // adaptive icons can't be loaded by painterResource, so compose the two vector layers
            Box(Modifier.size(52.dp).clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(52.dp).scale(1.5f))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(52.dp).scale(1.5f))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "NIGHT CITY",
                    color = p.accent.c(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                )
                Text(
                    "KEYBOARD // v" + helium314.keyboard.latin.BuildConfig.VERSION_NAME,
                    color = p.teal.c(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.nc_tagline), color = p.hintText.c(), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        // status lines, terminal style
        Text(stringResource(R.string.nc_status_label), color = p.hintText.c(), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        StatusLine(enabled, stringResource(if (enabled) R.string.nc_status_enabled else R.string.nc_status_not_enabled), p)
        StatusLine(current, stringResource(if (current) R.string.nc_status_default else R.string.nc_status_not_default), p)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val enableAction = {
                val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(intent) }
                Unit
            }
            if (!enabled)
                Button(onClick = enableAction, shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)) {
                    Text(stringResource(R.string.nc_enable_button))
                }
            else
                OutlinedButton(onClick = enableAction, shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                    border = BorderStroke(1.dp, p.border.c())) {
                    Text(stringResource(R.string.nc_enable_button), color = p.functionalText.c())
                }
            Button(
                onClick = { imm.showInputMethodPicker() },
                enabled = enabled,
                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                colors = if (current) ButtonDefaults.buttonColors(containerColor = p.keySecondary.c(), contentColor = p.teal.c())
                    else ButtonDefaults.buttonColors()
            ) {
                Text(stringResource(R.string.nc_select_button))
            }
        }
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String, p: NightCityPalette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(Modifier.size(8.dp).background(if (ok) p.teal.c() else Color(0xFFFF5C6C)))
        Spacer(Modifier.width(8.dp))
        Text(
            (if (ok) "[OK] " else "[--] ") + text,
            color = if (ok) p.functionalText.c() else p.hintText.c(),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** Three selectable visual profiles; writes both day and night theme preferences. */
@Composable
fun NightCityThemePicker() {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0) Log.v("irrelevant", "recompose")
    val selected = prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
    val p = currentNightCityPalette()
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(stringResource(R.string.nc_theme_title), p)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            NightCityPalette.ALL.forEach { palette ->
                val isSelected = palette.id == selected
                val borderColor by animateColorAsState(
                    if (isSelected) palette.accent.c() else palette.border.c(), tween(220), label = "border"
                )
                val scale by animateFloatAsState(if (isSelected) 1f else 0.96f, tween(220), label = "scale")
                Column(
                    Modifier
                        .weight(1f)
                        .scale(scale)
                        .background(palette.panel.c(), CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
                        .border(BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor), CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
                        .clickable {
                            prefs.edit {
                                putString(Settings.PREF_THEME_COLORS, palette.id)
                                putString(Settings.PREF_THEME_COLORS_NIGHT, palette.id)
                            }
                            KeyboardSwitcher.getInstance().setThemeNeedsReload()
                        }
                        .padding(10.dp)
                ) {
                    // swatches
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(palette.key, palette.accent, palette.teal, palette.keyText).forEach {
                            Box(Modifier.size(12.dp).background(it.c()).border(0.5.dp, palette.border.c()))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        palette.id.replace('_', ' ').uppercase(),
                        color = if (isSelected) palette.accent.c() else palette.functionalText.c(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(when (palette.id) {
                            NightCityPalette.AFTERLIFE -> R.string.nc_theme_desc_afterlife
                            NightCityPalette.NETRUNNER -> R.string.nc_theme_desc_netrunner
                            else -> R.string.nc_theme_desc_night_city
                        }),
                        color = palette.hintText.c(),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
        if (NightCityPalette.forId(selected) == null) {
            // a HeliBoard or user theme is active, make that visible
            Text(
                "> " + ctx.getString(R.string.theme_colors) + ": " + selected,
                color = p.hintText.c(), fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, p: NightCityPalette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Box(Modifier.size(width = 14.dp, height = 2.dp).background(p.accent.c()))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), color = p.functionalText.c(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
    }
}

/**
 * Static preview of the keyboard in the active profile. Uses the same palette, cut-corner geometry,
 * border and decoration settings as the real renderer, and the user's keyboard height scale.
 * It does not accept input (that's what [NightCityTestField] is for).
 */
@Composable
fun NightCityKeyboardPreview() {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val p = currentNightCityPalette()
    val borders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
    val fx = NightCityFxLevel.factor(prefs.getString(Settings.PREF_NC_EFFECT_LEVEL, Defaults.PREF_NC_EFFECT_LEVEL))
    val decor = prefs.getBoolean(Settings.PREF_NC_TECH_DECOR, Defaults.PREF_NC_TECH_DECOR) && fx > 0f
    val heightScale = Settings.readHeightScale(prefs, false, false).coerceIn(0.6f, 1.4f)
    val measurer = rememberTextMeasurer()
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(stringResource(R.string.nc_preview_title), p)
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f / heightScale)
        ) {
            val w = size.width
            val h = size.height
            // backdrop
            drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(p.panel.c(), p.base.c())))
            drawLine(p.border.c().copy(alpha = 0.55f), Offset(0f, 0f), Offset(w, 0f), 1.dp.toPx())
            if (decor) {
                drawLine(p.accent.c().copy(alpha = 0.6f), Offset(0f, 1.dp.toPx()), Offset(w * 0.08f, 1.dp.toPx()), 1.5.dp.toPx())
                drawLine(p.teal.c().copy(alpha = 0.6f), Offset(w - 22.dp.toPx(), 1.dp.toPx()), Offset(w - 12.dp.toPx(), 1.dp.toPx()), 1.5.dp.toPx())
            }
            // suggestion strip
            val stripH = h * 0.14f
            val words = listOf("привет", "Night", "City")
            words.forEachIndexed { i, word ->
                val color = when (i) { 1 -> p.accent; 0 -> p.functionalText; else -> p.keyText }
                val layout = measurer.measure(word, TextStyle(color = color.c(), fontSize = 13.sp))
                val cx = w * (i * 2 + 1) / 6f
                drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, (stripH - layout.size.height) / 2f))
            }
            
            val rows = listOf("йцукенгшщзх", "фывапролджэ", "^ячсмитьбю<", "#, ␣ .⏎")
            val keysTop = stripH + 4.dp.toPx()
            val rowH = (h - keysTop - 4.dp.toPx()) / rows.size
            val unit = w / 11f
            rows.forEachIndexed { r, row ->
                val y = keysTop + r * rowH
                if (r < 3) {
                    row.forEachIndexed { i, ch ->
                        val x = i * unit
                        val functional = ch == '^' || ch == '<'
                        val label = when (ch) { '^' -> "⇧"; '<' -> "⌫"; else -> ch.toString() }
                        previewKey(x, y, unit, rowH, label, if (functional) 2 else 1, p, borders, decor, measurer)
                    }
                } else {
                    // bottom row: symbols, comma, space, period, enter
                    val widths = listOf(1.6f, 1f, 5.2f, 1f, 2.2f)
                    val labels = listOf("?123", ",", "RU · EN", ".", "⏎")
                    val types = listOf(2, 1, 4, 1, 3)
                    var x = 0f
                    widths.forEachIndexed { i, wu ->
                        previewKey(x, y, unit * wu, rowH, labels[i], types[i], p, borders, decor, measurer)
                        x += unit * wu
                    }
                }
            }
        }
        Text(stringResource(R.string.nc_preview_note), color = p.hintText.c(), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

/** type: 1 normal, 2 functional, 3 action, 4 space */
private fun DrawScope.previewKey(
    x: Float, y: Float, w: Float, h: Float, label: String, type: Int, p: NightCityPalette,
    borders: Boolean, decor: Boolean, measurer: androidx.compose.ui.text.TextMeasurer,
) {
    val ix = 2.dp.toPx(); val iy = 2.8.dp.toPx()
    val l = x + ix; val t = y + iy; val r = x + w - ix; val b = y + h - iy
    val rad = minOf(7.dp.toPx(), minOf(r - l, b - t) * 0.28f)
    val cut = minOf(4.5.dp.toPx(), minOf(r - l, b - t) * 0.2f)
    val path = Path().apply {
        moveTo(l + cut, t); lineTo(r - rad, t); quadraticTo(r, t, r, t + rad)
        lineTo(r, b - rad); quadraticTo(r, b, r - rad, b)
        lineTo(l + rad, b); quadraticTo(l, b, l, b - rad)
        lineTo(l, t + cut); close()
    }
    val fill = when (type) { 2 -> p.keySecondary; 3 -> p.accent; 4 -> p.spaceBar; else -> p.key }
    drawPath(path, fill.c())
    if (borders && type != 3)
        drawPath(path, p.border.c(), style = Stroke(1.dp.toPx()))
    if (decor) {
        when (type) {
            2 -> drawCircle(p.teal.c().copy(alpha = 0.55f), 1.3.dp.toPx(), Offset(r - 4.5.dp.toPx(), t + 4.5.dp.toPx()))
            4 -> {
                val cx = (l + r) / 2f; val tw = maxOf(18.dp.toPx(), (r - l) * 0.12f)
                drawLine(p.accent.c().copy(alpha = 0.45f), Offset(cx - tw / 2, b - 4.dp.toPx()), Offset(cx + tw / 2, b - 4.dp.toPx()), 1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
    val color = when (type) { 3 -> p.onAccent; 2 -> p.functionalText; 4 -> p.hintText; else -> p.keyText }
    val fontSize = when (type) { 4 -> 10.sp; 2 -> if (label.length > 2) 10.sp else 15.sp; else -> 15.sp }
    val layout = measurer.measure(label, TextStyle(color = color.c(), fontSize = fontSize, fontWeight = if (type == 1) FontWeight.Medium else FontWeight.Normal))
    drawText(layout, topLeft = Offset((l + r - layout.size.width) / 2f, (t + b - layout.size.height) / 2f))
}

/** Real text field, so the user can open and try the actual keyboard. */
@Composable
fun NightCityTestField() {
    var text by rememberSaveable { mutableStateOf("") }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.nc_test_input_hint)) },
            singleLine = false,
            maxLines = 3,
        )
    }
}
