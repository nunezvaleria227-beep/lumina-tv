package com.luminatv.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminatv.tv.Config
import com.luminatv.tv.R

@Composable
fun LoginScreen(vm: AppViewModel) {
    val focusManager = LocalFocusManager.current
    val needsServer = Config.PORTAL_URL.isBlank()
    val state = vm.loginState
    var showPassword by remember { mutableStateOf(true) }
    val cardShape = RoundedCornerShape(22.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentLight,
        unfocusedBorderColor = LineColor,
        focusedLabelColor = AccentLight,
        unfocusedLabelColor = TextSecondary,
        cursorColor = AccentColor,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    AppBackground(dim = 0.30f) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ------------------------------------------------ banner de Lumina (mitad izquierda)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Image(
                    bitmap = rememberArt(R.drawable.login_art),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        // La foto se desvanece hacia la derecha y hacia abajo y se funde con el fondo, sin cortes.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.horizontalGradient(0.55f to Color.Black, 1f to Color.Transparent),
                                blendMode = BlendMode.DstIn
                            )
                            drawRect(
                                Brush.verticalGradient(0.72f to Color.Black, 1f to Color.Transparent),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            }

            // ------------------------------------------------ formulario
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    // tarjeta "de vidrio" detrás del formulario: se ve el fondo pero el texto se lee bien
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xC70C0410))
                    .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLogo(Modifier.width(250.dp))
                Text(
                    text = "Tus series y películas en un mismo lugar",
                    color = AccentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Inicia sesión",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Escribe el usuario y la contraseña que te dio tu proveedor.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                if (needsServer) {
                    OutlinedTextField(
                        value = vm.server,
                        onValueChange = { vm.server = it },
                        label = { Text("URL del servidor") },
                        placeholder = { Text("http://servidor.com:8080") },
                        singleLine = true,
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = vm.username,
                    onValueChange = { vm.username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vm.password,
                    onValueChange = { vm.password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    colors = fieldColors,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showPassword) "OCULTAR" else "VER",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showPassword = !showPassword }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            vm.submitLogin()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val error = state.error
                if (error != null) {
                    Text(
                        text = error,
                        color = ErrorColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TvButton(
                    text = if (state.loading) "Entrando… un momento" else "Entrar",
                    onClick = { vm.submitLogin() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!needsServer) {
                    Text(
                        text = "✓  El servidor ya viene configurado: solo necesitas tu usuario y contraseña.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
            }
        }
    }
}
