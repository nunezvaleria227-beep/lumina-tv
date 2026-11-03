package com.luminatv.tv.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminatv.tv.RecordingManager
import com.luminatv.tv.RecordingService
import com.luminatv.tv.RecordingConverter
import com.luminatv.tv.SavedRecording
import com.luminatv.tv.UpdateManager
import com.luminatv.tv.data.LiveChannel
import com.luminatv.tv.data.SubtitleSettings
import kotlinx.coroutines.delay
import java.io.File

/** Página sencilla con barra superior y contenido centrado. */
@Composable
private fun SimplePage(
    vm: AppViewModel,
    title: String,
    maxWidth: Dp = 600.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar(title = title, centerTitle = true, onBack = { vm.back() })
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

/** Título pequeño de una sección de Ajustes. */
@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        color = AccentLight,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp)
    )
}

/**
 * Fila de ajuste: nombre y explicación a la izquierda, valor actual y flecha a la derecha.
 * Al enfocarla se llena con el degradado rosa.
 */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.015f else 1f, label = "settingScale")
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            .clip(shape)
            .then(if (focused) Modifier.background(AccentBrush, shape) else Modifier.background(PanelColor.copy(alpha = 0.78f), shape))
            .border(1.dp, if (focused) Color.White.copy(alpha = 0.9f) else GlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (focused) Color.White else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = if (focused) Color.White.copy(alpha = 0.85f) else TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Spacer(Modifier.width(16.dp))
            Text(value, color = if (focused) Color.White else AccentLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        GlyphIcon(Glyph.CHEVRON, Modifier.size(16.dp), if (focused) Color.White else TextTertiary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanelColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

// --------------------------------------------------------------------- cuenta

@Composable
fun AccountScreen(vm: AppViewModel) {
    val account = vm.account
    val now = System.currentTimeMillis()
    val expires = account?.expiresAt?.let { formatDateLong(it * 1000) } ?: "Sin vencimiento"
    val daysLeft = account?.expiresAt?.let { (it * 1000 - now) / 86_400_000L }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        first.tryRequestFocus()
    }

    SimplePage(vm, "Mi cuenta") {
        Card {
            InfoRow("Usuario", account?.username ?: "—")
            InfoRow("Estado", account?.status ?: "—")
            InfoRow("Vence", expires)
            if (daysLeft != null) InfoRow("Días restantes", daysLeft.coerceAtLeast(0).toString())
            InfoRow("Conexiones", "${account?.activeConnections ?: "—"} de ${account?.maxConnections ?: "—"}")
        }
        TvButton(
            text = "Cerrar sesión",
            onClick = { vm.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(first)
        )
    }
}

// ---------------------------------------------------------------------- avisos

@Composable
fun NotificationsScreen(vm: AppViewModel) {
    val account = vm.account
    val now = System.currentTimeMillis()
    val expiresAt = account?.expiresAt
    val daysLeft = expiresAt?.let { (it * 1000 - now) / 86_400_000L }
    val message = account?.message

    SimplePage(vm, "Avisos") {
        var any = false
        if (!message.isNullOrBlank()) {
            any = true
            Card {
                Text("Mensaje de tu proveedor", color = AccentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(message, color = TextPrimary)
            }
        }
        if (daysLeft != null) {
            any = true
            val text = when {
                daysLeft < 0 -> "Tu cuenta venció el ${formatDateLong((expiresAt ?: 0L) * 1000)}."
                daysLeft == 0L -> "Tu cuenta vence hoy."
                daysLeft <= 7 -> "Tu cuenta vence en $daysLeft día(s), el ${formatDateLong((expiresAt ?: 0L) * 1000)}."
                else -> "Tu cuenta está activa hasta el ${formatDateLong((expiresAt ?: 0L) * 1000)} ($daysLeft días)."
            }
            Card {
                Text("Vencimiento", color = AccentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text, color = if (daysLeft <= 7) Color(0xFFFFC93C) else TextPrimary)
            }
        }
        val maxConnections = account?.maxConnections
        if (maxConnections != null) {
            any = true
            Card {
                Text("Conexiones", color = AccentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Tu cuenta permite $maxConnections conexión(es) a la vez. " +
                        "Si ves un error de \"máximo de conexiones\", cierra la reproducción en otro dispositivo.",
                    color = TextPrimary
                )
            }
        }
        if (!any) {
            Text("No hay avisos por ahora.", color = TextSecondary)
        }
    }
}

// --------------------------------------------------------------------- ajustes

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var confirm by remember { mutableStateOf<String?>(null) }
    var pinFlow by remember { mutableStateOf<PinFlow?>(null) }
    val context = LocalContext.current
    var subLang by remember { mutableStateOf(SubtitleSettings.language(context)) }
    var subAccount by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        first.tryRequestFocus()
    }
    val formatName = if (vm.liveFormat == "m3u8") "HLS (m3u8)" else "MPEG-TS"

    SimplePage(vm, "Ajustes", maxWidth = 720.dp) {
        val subLangName = when (subLang) {
            "es" -> "Español"
            "en" -> "Inglés"
            else -> "Apagados"
        }

        SettingsSection("Reproducción")
        SettingRow(
            title = "Formato de TV en vivo",
            subtitle = "Si un canal se corta o no abre, prueba cambiando el formato.",
            value = formatName,
            onClick = { vm.toggleLiveFormat() },
            modifier = Modifier.focusRequester(first)
        )
        SettingRow(
            title = "Subtítulos al empezar",
            subtitle = "Se activan solos en películas y series. También se cambian dentro del video (botón CC o tecla Menú).",
            value = subLangName,
            onClick = {
                subLang = when (subLang) {
                    "off" -> "es"
                    "es" -> "en"
                    else -> "off"
                }
                SubtitleSettings.setLanguage(context, subLang)
            }
        )
        SettingRow(
            title = "Subtítulos en internet",
            subtitle = "Si la película no trae subtítulos, la app los busca en OpenSubtitles y los guarda para la próxima vez.",
            value = "Configurar",
            onClick = { subAccount = true }
        )

        SettingsSection("Contenido")
        SettingRow(
            title = "Actualizar listas",
            subtitle = "Vuelve a descargar canales, películas y series. Se actualizan solas cada 12 horas.",
            value = "Actualizar ahora",
            onClick = { vm.refreshAll() }
        )
        ParentalSettings(vm, onFlow = { pinFlow = it })

        SettingsSection("Mis datos")
        SettingRow(
            title = "Borrar favoritos",
            subtitle = "Quita todos los canales, películas y series de Favoritos.",
            value = null,
            onClick = { confirm = "favoritos" }
        )
        SettingRow(
            title = "Borrar historial",
            subtitle = "Borra lo que viste recientemente y \"Continuar viendo\".",
            value = null,
            onClick = { confirm = "historial" }
        )

        SettingsSection("Aplicación")
        SettingRow(
            title = "Buscar actualización",
            subtitle = "Cuando hay una versión nueva, la app te avisa sola al abrirla.",
            value = "Versión " + UpdateManager.currentName(LocalContext.current),
            onClick = {
                val state = vm.update
                if (state is UpdateState.Available) {
                    vm.updateDismissed = false
                } else {
                    vm.checkForUpdate(manual = true)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
    }

    if (subAccount) SubtitleAccountDialog(onDismiss = { subAccount = false })

    PinFlowDialogs(vm, pinFlow, onFlow = { pinFlow = it }, toast = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() })

    val what = confirm
    if (what != null) {
        OptionsDialog(
            title = "¿Borrar $what?",
            message = "Esta acción no se puede deshacer.",
            options = listOf(
                Opt("Sí, borrar") { if (what == "favoritos") vm.clearFavorites() else vm.clearHistory() },
                Opt("Cancelar") { }
            ),
            onDismiss = { confirm = null }
        )
    }
}

// ------------------------------------------------------------- control parental

/** Pasos de las ventanas del PIN. */
sealed interface PinFlow {
    /** Elegir un PIN nuevo. */
    data object Create : PinFlow

    /** Repetir el PIN elegido. */
    data class Confirm(val first: String) : PinFlow

    /** Escribir el PIN para ver el contenido para adultos. */
    data object Unlock : PinFlow

    /** Comprobar el PIN antes de cambiarlo. */
    data object VerifyChange : PinFlow

    /** Comprobar el PIN antes de quitarlo. */
    data object VerifyRemove : PinFlow
}

@Composable
private fun ParentalSettings(vm: AppViewModel, onFlow: (PinFlow) -> Unit) {
    val parental = vm.parental
    if (!parental.hasPin) {
        SettingRow(
            title = "Control parental",
            subtitle = "Esconde las categorías para adultos (XXX, +18, adultos…) en listas, buscador y guía. Para verlas hace falta el PIN.",
            value = "Poner PIN",
            onClick = { onFlow(PinFlow.Create) }
        )
        return
    }
    if (parental.locked) {
        SettingRow(
            title = "Control parental activado",
            subtitle = "Ahora mismo el contenido para adultos está escondido.",
            value = "Ver con PIN",
            onClick = { onFlow(PinFlow.Unlock) }
        )
    } else {
        SettingRow(
            title = "Contenido para adultos visible",
            subtitle = "Se vuelve a esconder solo al cerrar sesión o al cerrar la app.",
            value = "Bloquear ahora",
            onClick = { vm.lockParental() }
        )
    }
    SettingRow(title = "Cambiar PIN", subtitle = null, value = null, onClick = { onFlow(PinFlow.VerifyChange) })
    SettingRow(title = "Quitar PIN", subtitle = null, value = null, onClick = { onFlow(PinFlow.VerifyRemove) })
}

@Composable
private fun PinFlowDialogs(vm: AppViewModel, flow: PinFlow?, onFlow: (PinFlow?) -> Unit, toast: (String) -> Unit) {
    val parental = vm.parental

    fun wrong(): String {
        val wait = parental.waitSeconds()
        return if (wait > 0) "Demasiados intentos. Espera $wait s." else "PIN incorrecto"
    }

    when (flow) {
        null -> Unit
        PinFlow.Create -> key(flow) {
            PinDialog(
                title = "Elige un PIN",
                message = "Escribe 4 números. Lo pedirá para ver el contenido para adultos.",
                onSubmit = { pin ->
                    onFlow(PinFlow.Confirm(pin))
                    null
                },
                onDismiss = { onFlow(null) }
            )
        }
        is PinFlow.Confirm -> key(flow) {
            PinDialog(
                title = "Repite el PIN",
                message = "Escríbelo otra vez para confirmarlo.",
                onSubmit = { pin ->
                    if (pin == flow.first) {
                        parental.setPin(pin)
                        if (parental.locked) vm.lockParental()
                        onFlow(null)
                        toast("Control parental activado. Anota tu PIN.")
                    } else {
                        toast("Los PIN no coinciden. Inténtalo de nuevo.")
                        onFlow(PinFlow.Create)
                    }
                    null
                },
                onDismiss = { onFlow(null) }
            )
        }
        PinFlow.Unlock -> key(flow) {
            PinDialog(
                title = "Escribe tu PIN",
                message = "Para ver el contenido para adultos.",
                onSubmit = { pin ->
                    if (parental.unlock(pin)) {
                        onFlow(null)
                        null
                    } else {
                        wrong()
                    }
                },
                onDismiss = { onFlow(null) }
            )
        }
        PinFlow.VerifyChange -> key(flow) {
            PinDialog(
                title = "Escribe tu PIN actual",
                message = "Después podrás elegir uno nuevo.",
                onSubmit = { pin ->
                    if (parental.verify(pin)) {
                        onFlow(PinFlow.Create)
                        null
                    } else {
                        wrong()
                    }
                },
                onDismiss = { onFlow(null) }
            )
        }
        PinFlow.VerifyRemove -> key(flow) {
            PinDialog(
                title = "Escribe tu PIN",
                message = "Para quitar el control parental.",
                onSubmit = { pin ->
                    if (parental.verify(pin)) {
                        parental.removePin()
                        onFlow(null)
                        toast("Control parental desactivado.")
                        null
                    } else {
                        wrong()
                    }
                },
                onDismiss = { onFlow(null) }
            )
        }
    }
}

// ------------------------------------------------------------------ grabaciones

@Composable
fun RecordingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val active = RecordingManager.current
    var files by remember { mutableStateOf(RecordingService.listSaved(context)) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var deleting by remember { mutableStateOf<SavedRecording?>(null) }
    var converting by remember { mutableStateOf<SavedRecording?>(null) }
    val convert = RecordingConverter.current

    // Cuando termina una grabación o una conversión se actualiza la lista; mientras graba se actualiza el tiempo.
    LaunchedEffect(active == null, convert == null) { files = RecordingService.listSaved(context) }

    // Convertir tarda: que la pantalla no se apague mientras tanto.
    val view = LocalView.current
    DisposableEffect(convert != null) {
        view.keepScreenOn = convert != null
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(active != null) {
        while (active != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    SimplePage(vm, "Grabaciones") {
        Text(
            "Cada grabación dura como máximo 2 horas. Al terminar se guarda sola en la memoria de este dispositivo (Películas / Lumina TV).",
            color = TextSecondary,
            fontSize = 12.sp
        )
        if (active != null) {
            Card {
                Text("● Grabando: ${active.channelName}", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                val elapsed = ((now - active.startedAt) / 1000).coerceAtLeast(0)
                val remaining = formatDuration(((active.endsAt - now) / 1000).coerceAtLeast(0))
                Text(
                    "${formatDuration(elapsed)} · ${formatSize(active.bytes)} · se guarda sola en $remaining",
                    color = TextPrimary
                )
                TvButton(text = "Detener y guardar", onClick = { RecordingManager.stop() }, modifier = Modifier.fillMaxWidth())
            }
        }

        if (convert != null) {
            Card {
                Text("⟳ ${convert.phase}: ${convert.title.replace('_', ' ')}", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "${convert.percent}% · ${formatSize(convert.outBytes)} · no cierres la app hasta que termine",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                TvButton(text = "Cancelar conversión", onClick = { RecordingConverter.cancel() }, modifier = Modifier.fillMaxWidth())
            }
        }

        val scheduled = vm.scheduledRecordings
        if (scheduled.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Programadas", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            scheduled.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Text(
                            "⏰ ${item.title}",
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${item.channelName} · ${formatDateLong(item.startSec * 1000)} ${formatClock(item.startSec * 1000)} – ${formatClock(item.endSec * 1000)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    TvButton(text = "Cancelar", onClick = { vm.cancelScheduled(context, item.id) }, compact = true)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (files.isEmpty()) {
            Text(
                "Todavía no hay grabaciones.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        files.forEach { recording ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocusCard(
                    onClick = { vm.playFile(context, recording.title, recording.url) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    scaleOnFocus = 1.01f
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            recording.title.replace('_', ' '),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${recording.ext.uppercase()} · ${formatSize(recording.size)} · ${formatDateLong(recording.dateMs)} ${formatClock(recording.dateMs)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                if (recording.ext != "mp4" && convert == null) {
                    TvButton(text = "A MP4", onClick = { converting = recording }, compact = true)
                }
                TvButton(text = "Borrar", onClick = { deleting = recording }, compact = true)
            }
        }
    }

    val toConvert = converting
    if (toConvert != null) {
        OptionsDialog(
            title = "Convertir a MP4",
            message = "Crea una copia en MP4 que se abre en cualquier celular o programa. Puede tardar unos minutos y necesita espacio libre de unas 2 veces el tamaño de la grabación (${formatSize(toConvert.size)}).",
            options = listOf(
                Opt("Convertir y conservar la original") { RecordingConverter.start(context, toConvert, deleteOriginal = false) },
                Opt("Convertir y borrar la original") { RecordingConverter.start(context, toConvert, deleteOriginal = true) },
                Opt("Cancelar") { }
            ),
            onDismiss = { converting = null }
        )
    }

    val toDelete = deleting
    if (toDelete != null) {
        OptionsDialog(
            title = "¿Borrar esta grabación?",
            message = toDelete.title.replace('_', ' '),
            options = listOf(
                Opt("Sí, borrar") {
                    if (!RecordingService.delete(context, toDelete)) {
                        Toast.makeText(context, "No se pudo borrar. Bórrala desde la app de Archivos.", Toast.LENGTH_LONG).show()
                    }
                    files = RecordingService.listSaved(context)
                },
                Opt("Cancelar") { }
            ),
            onDismiss = { deleting = null }
        )
    }
}
