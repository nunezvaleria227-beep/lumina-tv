package com.luminatv.tv.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.luminatv.tv.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Posiciones del splash (fracciones de una pantalla 16:9). Salen de art/make_assets.py,
// el mismo programa que dibujó el logo en 4K, para que todo quede exactamente alineado.
private const val WORD_WIDTH = 0.66f
private const val WORD_ASPECT = 2534f / 507f
private const val WORD_CENTER_Y = 0.475f
private const val STAR_X = 0.46912f
private const val STAR_Y = 0.37701f
private const val STAR_SIZE = 0.13620f
private const val TAG_WIDTH = 0.57318f
private const val TAG_ASPECT = 2201f / 162f
private const val TAG_TOP = 0.57384f
private const val DOTS_Y = 0.700f

/** Tiempo que se muestra el logo antes de entrar (si nadie toca el control). */
private const val SPLASH_MILLIS = 3000L

/**
 * Pantalla de inicio de Lumina TV con el logo en 4K (3840x2160).
 *
 * El fondo es el mismo que se ve desde el primer instante (fondo de la ventana), así no hay
 * parpadeo. Encima entra la palabra LUMINA TV con un brillo que la recorre, la estrella gira y
 * late, y aparece el lema. Cualquier botón del control la salta.
 */
@Composable
fun LuminaSplash(skip: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val targetWidth = remember { screenWidthPx(context) }
    val bg = rememberSampledBitmap(R.drawable.splash_bg, targetWidth)
    val word = rememberSampledBitmap(R.drawable.splash_word, (targetWidth * WORD_WIDTH).toInt())
    val star = rememberSampledBitmap(R.drawable.splash_star, (targetWidth * STAR_SIZE).toInt())
    val tag = rememberSampledBitmap(R.drawable.splash_tagline, (targetWidth * TAG_WIDTH).toInt())

    val wordIn = remember { Animatable(0f) }
    val starIn = remember { Animatable(0f) }
    val shine = remember { Animatable(-0.35f) }
    val tagIn = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val skipNow by rememberUpdatedState(skip)
    val finish by rememberUpdatedState(onFinished)

    val loop = rememberInfiniteTransition(label = "splashLoop")
    val pulse by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val dotsPhase by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "dots"
    )

    LaunchedEffect(Unit) {
        launch {
            delay(120)
            wordIn.animateTo(1f, tween(1150, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }
        launch {
            delay(620)
            starIn.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 90f))
        }
        launch {
            delay(950)
            shine.animateTo(1.35f, tween(1250, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
        }
        launch {
            delay(1300)
            tagIn.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        }
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < SPLASH_MILLIS && !skipNow) delay(40)
        exit.animateTo(1f, tween(if (skipNow) 260 else 520, easing = FastOutSlowInEasing))
        finish()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer {
                alpha = 1f - exit.value
                val s = 1f + 0.05f * exit.value
                scaleX = s
                scaleY = s
            }
            .background(BackgroundColor)
    ) {
        // Escenario 16:9 que cubre la pantalla (en pantallas más anchas se recorta arriba y abajo).
        val stageW: Dp = max(maxWidth, maxHeight * (16f / 9f))
        val stageH: Dp = stageW * (9f / 16f)
        Box(
            Modifier
                .requiredSize(stageW, stageH)
                .align(Alignment.Center)
        ) {
            if (bg != null) {
                Image(
                    bitmap = bg,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Resplandor que respira detrás del logo
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * 0.5f, size.height * WORD_CENTER_Y)
                val r = size.width * (0.30f + 0.03f * pulse)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFF2E93).copy(alpha = 0.16f * wordIn.value * (0.75f + 0.25f * pulse)),
                            Color.Transparent
                        ),
                        center = c,
                        radius = r
                    ),
                    radius = r,
                    center = c
                )
            }

            // LUMINA TV
            if (word != null) {
                val w = stageW * WORD_WIDTH
                val h = w / WORD_ASPECT
                Image(
                    bitmap = word,
                    contentDescription = "Lumina TV",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(x = (stageW - w) / 2, y = stageH * WORD_CENTER_Y - h / 2)
                        .size(w, h)
                        .graphicsLayer {
                            alpha = wordIn.value.coerceIn(0f, 1f)
                            val s = 0.90f + 0.10f * wordIn.value
                            scaleX = s
                            scaleY = s
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            // destello que recorre las letras
                            val p = shine.value
                            if (p > -0.3f && p < 1.3f) {
                                val cx = size.width * p
                                val band = size.width * 0.12f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        0f to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.75f),
                                        1f to Color.Transparent,
                                        start = Offset(cx - band, 0f),
                                        end = Offset(cx + band * 0.4f, size.height)
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                )
            }

            // Estrella
            if (star != null) {
                val s = stageW * STAR_SIZE
                Image(
                    bitmap = star,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(x = stageW * STAR_X - s / 2, y = stageH * STAR_Y - s / 2)
                        .size(s, s)
                        .graphicsLayer {
                            val v = starIn.value
                            alpha = (v * 1.6f).coerceIn(0f, 1f)
                            val k = v.coerceAtLeast(0f) * (1f + 0.10f * pulse)
                            scaleX = k
                            scaleY = k
                            rotationZ = (1f - v) * -140f + 5f * (pulse - 0.5f)
                        }
                )
            }

            // Lema
            if (tag != null) {
                val w = stageW * TAG_WIDTH
                val h = w / TAG_ASPECT
                Image(
                    bitmap = tag,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(x = (stageW - w) / 2, y = stageH * TAG_TOP)
                        .size(w, h)
                        .graphicsLayer {
                            alpha = tagIn.value
                            translationY = (1f - tagIn.value) * size.height * 0.6f
                        }
                )
            }

            // Tres puntos de "cargando"
            Canvas(Modifier.fillMaxSize()) {
                val r = size.height * 0.0055f
                val gap = r * 4.2f
                val y = size.height * DOTS_Y
                for (i in 0..2) {
                    val x = size.width / 2 + (i - 1) * gap
                    val ph = ((dotsPhase - i * 0.18f) % 1f + 1f) % 1f
                    val bump = if (ph < 0.5f) ph * 2f else (1f - ph) * 2f
                    drawCircle(
                        color = Color(0xFFFF5FB0).copy(alpha = tagIn.value * (0.25f + 0.75f * bump)),
                        radius = r * (0.85f + 0.3f * bump),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/** Ancho real de la pantalla en píxeles (en una tele 4K, 3840 aunque la interfaz sea 1080p). */
private fun screenWidthPx(context: Context): Int {
    val dm = context.resources.displayMetrics
    var w = maxOf(dm.widthPixels, dm.heightPixels)
    if (Build.VERSION.SDK_INT >= 23) {
        try {
            @Suppress("DEPRECATION")
            val display = context.findActivity()?.windowManager?.defaultDisplay
            val mode = display?.mode
            if (mode != null) w = maxOf(w, mode.physicalWidth, mode.physicalHeight)
        } catch (e: Exception) {
            // nos quedamos con el tamaño de la interfaz
        }
    }
    return w.coerceIn(960, 3840)
}

/**
 * Decodifica una imagen grande solo al tamaño que la pantalla necesita: en una tele 4K se usa
 * completa y en una de 1080p a la mitad (menos memoria y abre más rápido).
 */
@Composable
private fun rememberSampledBitmap(@DrawableRes id: Int, targetWidthPx: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    return remember(id, targetWidthPx) {
        fun decode(sample: Int): ImageBitmap? {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inScaled = false
            }
            return BitmapFactory.decodeResource(resources, id, opts)?.asImageBitmap()
        }
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, id, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidthPx) sample *= 2
            try {
                decode(sample)
            } catch (oom: OutOfMemoryError) {
                decode(sample * 2)
            }
        } catch (e: Throwable) {
            null
        }
    }
}
