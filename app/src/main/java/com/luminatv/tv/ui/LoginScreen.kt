package com.luminatv.tv.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminatv.tv.Config
import com.luminatv.tv.R

private val NeonNight = Color(0xFF0A0310)
private val LabelColor = Color(0xFFA88FA4)
private val HintColor = Color(0xFF8E7A8B)
private val FieldShape = RoundedCornerShape(10.dp)

/**
 * Entrar a Lumina TV (diseño "Neón"): el logo grande arriba con un brillo rosa y,
 * al centro, solo usuario, contraseña y el botón para entrar.
 */
@Composable
fun LoginScreen(vm: AppViewModel) {
    val focusManager = LocalFocusManager.current
    val needsServer = Config.PORTAL_URL.isBlank()
    val state = vm.loginState

    Box(
        Modifier
            .fillMaxSize()
            .background(NeonNight)
    ) {
        Image(
            bitmap = rememberArt(R.drawable.bg_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.9f }
        )
        // Brillo rosa detrás del logo y fondo oscuro y limpio hacia abajo.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to AccentColor.copy(alpha = 0.26f),
                        1f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(960f, 300f),
                        radius = 1000f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to NeonNight.copy(alpha = 0.15f),
                        0.4f to NeonNight.copy(alpha = 0.2f),
                        0.9f to NeonNight
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (needsServer) 34.dp else 58.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo con su brillo.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(380.dp, 110.dp)
                        .background(
                            Brush.radialGradient(
                                0f to AccentColor.copy(alpha = 0.35f),
                                1f to Color.Transparent
                            )
                        )
                )
                AppLogo(Modifier.width(264.dp))
            }
            Spacer(Modifier.height(if (needsServer) 10.dp else 26.dp))

            Column(Modifier.width(320.dp)) {
                if (needsServer) {
                    NeonField(
                        label = "SERVIDOR",
                        value = vm.server,
                        onValueChange = { vm.server = it },
                        placeholder = "http://servidor.com:8080",
                        icon = Icons.Filled.LocationOn,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                        onAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                }
                NeonField(
                    label = "USUARIO",
                    value = vm.username,
                    onValueChange = { vm.username = it },
                    placeholder = "Escribe tu usuario",
                    icon = Icons.Filled.Person,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onAction = { focusManager.moveFocus(FocusDirection.Down) }
                )
                NeonField(
                    label = "CONTRASEÑA",
                    value = vm.password,
                    onValueChange = { vm.password = it },
                    placeholder = "Escribe tu contraseña",
                    icon = Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onAction = {
                        focusManager.clearFocus()
                        vm.submitLogin()
                    }
                )

                val error = state.error
                if (error != null) {
                    Text(
                        text = error,
                        color = ErrorColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                Spacer(Modifier.height(if (error != null) 12.dp else 20.dp))
                EnterButton(
                    text = if (state.loading) "Entrando… un momento" else "Entrar",
                    enabled = !state.loading,
                    onClick = { vm.submitLogin() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Smart TV", "Fire TV Stick", "Android TV", "Roku").forEach { device ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Star4(7.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(device, color = LabelColor, fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeonField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onAction: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = label,
        color = LabelColor,
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 13.dp, bottom = 5.dp)
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LuminaFont
        ),
        cursorBrush = SolidColor(AccentLight),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onAction() }, onDone = { onAction() }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(41.dp)
                    .shadow(if (focused) 14.dp else 0.dp, FieldShape, ambientColor = AccentColor, spotColor = AccentColor)
                    .clip(FieldShape)
                    .background(if (focused) AccentColor.copy(alpha = 0.15f) else Color(0x12FFFFFF))
                    .border(
                        if (focused) 1.5.dp else 0.5.dp,
                        if (focused) AccentLight else Color(0x29FFFFFF),
                        FieldShape
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (focused) Color(0xFFFF9ED0) else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = HintColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
private fun EnterButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, label = "enter")
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            .shadow(if (focused) 22.dp else 12.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFFF4FA3), Color(0xFFC2167F))))
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(42.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (enabled) {
            Spacer(Modifier.width(8.dp))
            Canvas(Modifier.size(13.dp)) {
                val w = size.width
                val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                val arrow = Path().apply {
                    moveTo(w * 0.2f, w / 2)
                    lineTo(w * 0.8f, w / 2)
                    moveTo(w * 0.55f, w * 0.25f)
                    lineTo(w * 0.8f, w / 2)
                    lineTo(w * 0.55f, w * 0.75f)
                }
                drawPath(arrow, Color.White, style = stroke)
            }
        }
    }
}

/** Destello de cuatro puntas (como en el logo). */
@Composable
private fun Star4(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w / 2, 0f)
            lineTo(w * 0.61f, h * 0.39f)
            lineTo(w, h / 2)
            lineTo(w * 0.61f, h * 0.61f)
            lineTo(w / 2, h)
            lineTo(w * 0.39f, h * 0.61f)
            lineTo(0f, h / 2)
            lineTo(w * 0.39f, h * 0.39f)
            close()
        }
        drawPath(path, Color(0xFFFF9ED0))
    }
}
