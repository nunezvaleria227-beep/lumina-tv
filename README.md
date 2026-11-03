# Lumina TV — app para Fire TV y Android

Reproductor de TV en vivo, películas y series para Fire TV, Fire TV Stick y celulares Android.
Entra con usuario y contraseña; el servidor ya viene configurado (`app/src/main/java/com/luminatv/tv/Config.kt`).

- Logo de inicio animado en 4K (3840×2160): `app/src/main/res/drawable-nodpi/splash_*`.
- Colores, logo y banner propios de Lumina TV (morado y rosa).
- Es una app aparte: se instala junto a las demás sin reemplazar ninguna.

## Descarga

Enlace estable al último APK (para Downloader):

https://github.com/nunezvaleria227-beep/lumina-tv/releases/latest/download/LuminaTV.apk

## Cómo se compila

Cada cambio en `main` compila el APK en GitHub Actions (`.github/workflows/lumina-apk.yml`)
y lo publica en Releases. La app busca sola las versiones nuevas en ese mismo enlace.

Los gráficos (logo 4K, splash, banner de Fire TV, icono y fondos) se generan con
`art/make_logo.py` y `art/make_assets.py` a partir de los archivos originales de `art/`.
