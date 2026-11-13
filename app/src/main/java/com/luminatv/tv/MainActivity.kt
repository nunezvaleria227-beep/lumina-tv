package com.luminatv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.Modifier
import com.luminatv.tv.ui.AccountScreen
import com.luminatv.tv.ui.AppViewModel
import com.luminatv.tv.ui.ContinueScreen
import com.luminatv.tv.ui.CatchUpChannelsScreen
import com.luminatv.tv.ui.CatchUpProgramsScreen
import com.luminatv.tv.ui.CatchUpScreen
import com.luminatv.tv.ui.EpgCategoriesScreen
import com.luminatv.tv.ui.EpgChannelsScreen
import com.luminatv.tv.ui.EpgGridScreen
import com.luminatv.tv.ui.HomeScreen
import com.luminatv.tv.ui.LiveScreen
import com.luminatv.tv.ui.LoginScreen
import com.luminatv.tv.ui.LuminaSplash
import com.luminatv.tv.ui.MiTvTheme
import com.luminatv.tv.ui.MovieDetailScreen
import com.luminatv.tv.ui.MultiScreen
import com.luminatv.tv.ui.NotificationsScreen
import com.luminatv.tv.ui.PosterGridScreen
import com.luminatv.tv.ui.RecordingsScreen
import com.luminatv.tv.ui.Screen
import com.luminatv.tv.ui.qaName
import com.luminatv.tv.ui.SearchScope
import com.luminatv.tv.ui.SearchScreen
import com.luminatv.tv.ui.SeriesDetailScreen
import com.luminatv.tv.ui.SettingsScreen
import com.luminatv.tv.data.Kind

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (Config.QA_BUILD) qaOpen(intent)
    }

    private fun qaOpen(intent: android.content.Intent?) {
        MainActivityQa(viewModel, intent?.getStringExtra("qa_open"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firstLaunch = savedInstanceState == null
        if (Config.QA_BUILD) qaOpen(intent)
        setContent {
            MiTvTheme {
                // Logo de inicio en 4K encima de la app mientras esta carga por debajo.
                var showSplash by rememberSaveable { mutableStateOf(firstLaunch) }
                var skipSplash by remember { mutableStateOf(false) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (!showSplash) return@onPreviewKeyEvent false
                            // Durante el logo cualquier botón lo salta (y no llega a la pantalla de abajo).
                            if (event.type == KeyEventType.KeyDown) skipSplash = true
                            true
                        }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppRoot(viewModel)
                    LastCrashNotice()
                    }
                    if (showSplash) {
                        LuminaSplash(skip = skipSplash) {
                            showSplash = false
                            // Ya no hace falta la imagen de fondo de la ventana: se libera memoria.
                            window.setBackgroundDrawableResource(R.color.background)
                        }
                    }
                }
            }
        }
        if (!firstLaunch) window.setBackgroundDrawableResource(R.color.background)
    }
}

/** Solo en la compilación de prueba: abre una pantalla pedida con "am start --es qa_open Movies". */
private fun MainActivityQa(vm: AppViewModel, name: String?) {
    val screen = when (name) {
        "Live" -> Screen.Live
        "Movies" -> Screen.Movies
        "SeriesList" -> Screen.SeriesList
        "Settings" -> Screen.Settings
        "Search" -> Screen.Search(SearchScope.ALL)
        else -> null
    } ?: return
    vm.qaOpen(screen)
}

/** Si la app se cerró sola la vez anterior, muestra el motivo una vez (para mandarlo en foto). */
@Composable
private fun LastCrashNotice() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(CrashLog.read(context)) }
    val shown = text ?: return
    com.luminatv.tv.ui.OptionsDialog(
        title = "La app se cerró la última vez",
        message = "Toma una foto de este mensaje y mándala a soporte:\n\n$shown",
        options = listOf(com.luminatv.tv.ui.Opt("Entendido") { CrashLog.clear(context); text = null }),
        onDismiss = { CrashLog.clear(context); text = null }
    )
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    // Atrás en cualquier pantalla abierta regresa a la anterior; en el inicio o el login cierra la app.
    BackHandler(enabled = vm.stack.size > 1) { vm.back() }

    val current = vm.stack.last()
    if (Config.QA_BUILD) {
        androidx.compose.runtime.LaunchedEffect(current) {
            androidx.compose.runtime.withFrameNanos { }
            QaProbe.shown(current.qaName())
        }
    }
    when (val top = current) {
        Screen.Login -> LoginScreen(vm)
        Screen.Home -> HomeScreen(vm)
        Screen.Live -> LiveScreen(vm, vm.live, title = "TV en vivo")
        Screen.Radio -> LiveScreen(vm, vm.radio, title = "Radio", radio = true)
        Screen.Movies -> PosterGridScreen(
            vm = vm,
            state = vm.movies,
            kind = Kind.MOVIES,
            searchScope = SearchScope.MOVIES,
            noun = "películas",
            art = R.drawable.art_movies,
            posterOf = { it.poster },
            onOpen = { vm.openMovie(it) }
        )
        Screen.SeriesList -> PosterGridScreen(
            vm = vm,
            state = vm.series,
            kind = Kind.SERIES,
            searchScope = SearchScope.SERIES,
            noun = "series",
            art = R.drawable.art_series,
            posterOf = { it.cover },
            onOpen = { vm.openSeries(it) }
        )
        is Screen.MovieDetails -> MovieDetailScreen(vm, top.movie)
        is Screen.SeriesDetails -> SeriesDetailScreen(vm, top.series)
        Screen.Epg -> EpgCategoriesScreen(vm)
        is Screen.EpgChannels -> EpgChannelsScreen(vm, top.categoryId, top.title)
        is Screen.EpgGrid -> EpgGridScreen(vm, top.categoryId, top.title)
        Screen.CatchUp -> CatchUpScreen(vm)
        is Screen.CatchUpChannels -> CatchUpChannelsScreen(vm, top.categoryId, top.title)
        is Screen.CatchUpPrograms -> CatchUpProgramsScreen(vm, top.channel)
        Screen.Multi -> MultiScreen(vm)
        is Screen.Search -> SearchScreen(vm, top.scope)
        Screen.Account -> AccountScreen(vm)
        Screen.Notifications -> NotificationsScreen(vm)
        Screen.Settings -> SettingsScreen(vm)
        Screen.Recordings -> RecordingsScreen(vm)
        Screen.Continue -> ContinueScreen(vm)
    }
}
