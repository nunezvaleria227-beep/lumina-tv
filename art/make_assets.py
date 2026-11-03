"""Genera todos los graficos de Lumina TV a partir de out/*_master.png.

  res/  -> recursos que van dentro de la app (drawable-nodpi)
  preview/ -> imagenes completas para revisar (splash 4K compuesto, etc.)
"""
import json, os, random
import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFont, ImageFilter

random.seed(7)
np.random.seed(7)
RES, PREV = "res", "preview"
os.makedirs(RES, exist_ok=True)
os.makedirs(PREV, exist_ok=True)
L = json.load(open("out/layout.json"))
WORD = Image.open("out/wordmark_master.png")
STAR = Image.open("out/star_master.png")
LOGO = Image.open("out/logo_master.png")
POP_MED = "/usr/share/fonts/truetype/google-fonts/Poppins-Medium.ttf"
POP_LIGHT = "/usr/share/fonts/truetype/google-fonts/Poppins-Light.ttf"
if not os.path.exists(POP_MED):
    POP_MED = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"
if not os.path.exists(POP_LIGHT):
    POP_LIGHT = POP_MED

# Paleta Lumina
PLUM_EDGE = np.array([8, 1, 11], np.float32)
PLUM_MID = np.array([20, 4, 25], np.float32)
PLUM_CENTER = np.array([38, 7, 40], np.float32)
MAGENTA = np.array([255, 46, 149], np.float32)


def smoothstep(x):
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x)


def plum_background(w, h, cx=0.5, cy=0.47, glow=(0.5, 0.47, 0.42, 0.16, 0.55), ring=None, particles=0, seed=1):
    """Fondo morado profundo con brillo magenta, aro opcional y particulas."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    nx, ny = xx / w, yy / h
    d = np.sqrt(((nx - cx) * 1.0) ** 2 + ((ny - cy) * (h / w)) ** 2 * 1.0) / 0.75
    t = smoothstep(d)
    col = PLUM_CENTER[None, None] * (1 - t[..., None]) + PLUM_EDGE[None, None] * t[..., None]
    col = col * 0.55 + (PLUM_MID[None, None] * (1 - t[..., None]) + PLUM_EDGE[None, None] * t[..., None]) * 0.45
    # brillo magenta eliptico
    gx, gy, rx, ry, ga = glow
    g = np.exp(-(((nx - gx) / rx) ** 2 + ((ny - gy) / ry) ** 2))
    col = col + (np.array([120, 14, 78], np.float32) - col) * (g * ga)[..., None]
    img = col
    # aro brillante (como el del banner)
    if ring is not None:
        rcx, rcy, rr, rw, ra, ang0, ang1 = ring
        dist = np.sqrt(((xx - rcx * w)) ** 2 + ((yy - rcy * h)) ** 2)
        band = np.exp(-((dist - rr * h) / (rw * h)) ** 2)
        halo = np.exp(-((dist - rr * h) / (rw * h * 9)) ** 2) * 0.35
        ang = np.degrees(np.arctan2(yy - rcy * h, xx - rcx * w)) % 360.0
        rel = (ang - ang0) % 360.0
        span = ang1 - ang0
        fade = smoothstep(rel / (span * 0.35)) * smoothstep((span - rel) / (span * 0.45)) * (rel <= span)
        a = (band + halo) * fade * ra
        ring_col = np.array([255, 60, 165], np.float32)
        img = img + (ring_col - img) * np.clip(a, 0, 1)[..., None]
    # particulas suaves
    if particles:
        layer = np.zeros((h, w), np.float32)
        for _ in range(particles):
            px, py = rng.uniform(0, w), rng.uniform(0, h)
            # evitar el centro (donde va el logo)
            if abs(px / w - 0.5) < 0.36 and abs(py / h - 0.48) < 0.22:
                continue
            r = rng.uniform(1.2, 4.2) * (w / 3840)
            a = rng.uniform(0.15, 0.7)
            cv2.circle(layer, (int(px), int(py)), max(1, int(r)), a, -1, lineType=cv2.LINE_AA)
        layer = cv2.GaussianBlur(layer, (0, 0), max(1.0, 2.5 * w / 3840)) * 1.6
        img = img + (np.array([255, 150, 210], np.float32) - img) * np.clip(layer, 0, 0.6)[..., None]
    # vineta
    v = np.sqrt((nx - 0.5) ** 2 + (ny - 0.5) ** 2) / 0.72
    img = img * (1 - 0.45 * smoothstep(v) ** 1.6)[..., None]
    # tramado para que el degradado no haga escalones
    img = img + rng.uniform(-1.6, 1.6, img.shape).astype(np.float32)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")


def scaled(im, width):
    h = round(im.height * width / im.width)
    return im.resize((width, h), Image.LANCZOS)


def text_layer(text, font_path, size, tracking, color, pad=40):
    font = ImageFont.truetype(font_path, size)
    # ancho con espaciado entre letras
    widths = [font.getlength(ch) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    asc, desc = font.getmetrics()
    im = Image.new("RGBA", (int(total) + 2 * pad, asc + desc + 2 * pad), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    x = pad
    for ch, wch in zip(text, widths):
        d.text((x, pad), ch, font=font, fill=color)
        x += wch + tracking
    glow = im.filter(ImageFilter.GaussianBlur(size * 0.35))
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ga = np.asarray(glow).astype(np.float32)
    ga[..., :3] = [255, 60, 160]
    ga[..., 3] *= 0.5
    out.alpha_composite(Image.fromarray(ga.astype(np.uint8), "RGBA"))
    out.alpha_composite(im)
    return out


# ------------------------------------------------------------------ splash 4K
SW, SH = 3840, 2160
splash_bg = plum_background(SW, SH, glow=(0.5, 0.47, 0.40, 0.17, 0.62),
                            ring=(0.5, 0.47, 0.80, 0.0016, 0.60, 95, 255), particles=260, seed=3)
splash_bg.save(f"{RES}/splash_bg.jpg", quality=92, subsampling=0, optimize=True)
splash_bg.resize((1920, 1080), Image.LANCZOS).save(f"{RES}/splash_window.jpg", quality=90, subsampling=0, optimize=True)

WORD_FRAC = 0.66                       # ancho del logo (con margen) respecto a la pantalla
ww = round(SW * WORD_FRAC)
k = ww / WORD.width
word = scaled(WORD, ww)
star = scaled(STAR, round(STAR.width * k))
word.save(f"{RES}/splash_word.png", optimize=True)
star.save(f"{RES}/splash_star.png", optimize=True)
tag = text_layer("TUS SERIES Y PELÍCULAS EN UN MISMO LUGAR", POP_MED, 58, 22, (244, 214, 236, 235))
tag.save(f"{RES}/splash_tagline.png", optimize=True)

# posiciones (fracciones de pantalla) que usa la app para armar el splash
word_cy = 0.475
wx0 = (SW - word.width) / 2
wy0 = SH * word_cy - word.height / 2
scx = wx0 + L["star_center_in_wordmark"][0] * k
scy = wy0 + L["star_center_in_wordmark"][1] * k
tag_y = wy0 + word.height - 40
splash_layout = {
    "word_width_frac": WORD_FRAC,
    "word_aspect": word.width / word.height,
    "word_center_y_frac": word_cy,
    "star_center_frac": [scx / SW, scy / SH],
    "star_width_frac": star.width / SW,
    "tag_width_frac": tag.width / SW,
    "tag_top_frac": tag_y / SH,
}
json.dump(splash_layout, open(f"{RES}/splash_layout.json", "w"), indent=1)

comp = splash_bg.convert("RGBA")
comp.alpha_composite(word, (round(wx0), round(wy0)))
comp.alpha_composite(star, (round(scx - star.width / 2), round(scy - star.height / 2)))
comp.alpha_composite(tag, (round((SW - tag.width) / 2), round(tag_y)))
comp.convert("RGB").save(f"{PREV}/LuminaTV_splash_4K.png", optimize=True)

# ------------------------------------------------------------------ logo para la app
logo_app = scaled(LOGO.crop((150, 190, 4210, 1110)), 1600)  # recorte justo: el logo se ve más grande
logo_app.save(f"{RES}/logo_lumina.png", optimize=True)
# marca del menu lateral: la estrella sola, recortada
st = STAR.copy()
bbox = st.getbbox()
st = st.crop(bbox)
side = max(st.size)
sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
sq.alpha_composite(st, ((side - st.width) // 2, (side - st.height) // 2))
sq.resize((256, 256), Image.LANCZOS).save(f"{RES}/logo_mark.png", optimize=True)

# ------------------------------------------------------------------ banner Fire TV (1280x720) e icono
def card(w, h, logo_w, seed, tagline=None, cy=0.5):
    bg = plum_background(w, h, cy=cy, glow=(0.5, cy, 0.45, 0.22, 0.65),
                         ring=(0.5, cy, 0.86, 0.003, 0.55, 95, 255), particles=int(60 * w / 1280), seed=seed).convert("RGBA")
    lg = scaled(LOGO, logo_w)
    y = round(h * cy - lg.height / 2)
    bg.alpha_composite(lg, ((w - lg.width) // 2, y))
    if tagline:
        t = scaled(text_layer(tagline, POP_MED, 58, 22, (244, 214, 236, 235)), round(w * 0.62))
        bg.alpha_composite(t, ((w - t.width) // 2, y + lg.height - round(h * 0.06)))
    return bg.convert("RGB")


card(1280, 720, 1180, 5, cy=0.50).save(f"{RES}/banner.png", optimize=True)
card(1024, 1024, 960, 9, cy=0.50).save(f"{RES}/ic_launcher.png", optimize=True)

# ------------------------------------------------------------------ fondos de pantallas (1920x1080)
plum_background(1920, 1080, cx=0.75, cy=0.25, glow=(0.85, 0.15, 0.45, 0.35, 0.45),
                ring=(0.92, 0.95, 0.72, 0.0018, 0.35, 150, 300), particles=70, seed=11) \
    .resize((1280, 720), Image.LANCZOS).save(f"{RES}/bg_home.jpg", quality=90, subsampling=0, optimize=True)
plum_background(1920, 1080, cx=0.2, cy=0.2, glow=(0.15, 0.1, 0.5, 0.35, 0.50),
                ring=(0.05, 1.0, 0.85, 0.0018, 0.35, 250, 400), particles=70, seed=12) \
    .resize((1280, 720), Image.LANCZOS).save(f"{RES}/art_live.jpg", quality=90, subsampling=0, optimize=True)
plum_background(1920, 1080, cx=0.8, cy=0.2, glow=(0.9, 0.05, 0.5, 0.35, 0.50),
                ring=(1.0, 1.0, 0.85, 0.0018, 0.35, 180, 290), particles=70, seed=13) \
    .resize((1280, 720), Image.LANCZOS).save(f"{RES}/art_movies.jpg", quality=90, subsampling=0, optimize=True)
plum_background(1920, 1080, cx=0.5, cy=0.0, glow=(0.5, 0.0, 0.6, 0.35, 0.50),
                ring=(0.5, 1.25, 1.0, 0.0018, 0.35, 200, 340), particles=70, seed=14) \
    .resize((1280, 720), Image.LANCZOS).save(f"{RES}/art_series.jpg", quality=90, subsampling=0, optimize=True)

# ------------------------------------------------------------------ imagen del login (del banner del usuario)
promo = Image.open("promo_src.png").convert("RGB")
# la mitad izquierda (persona + aro); proporcion de media pantalla 16:9 -> 0.889
ph = promo.height
pw = 745
x0 = 215
login = promo.crop((x0, 0, x0 + pw, ph)).resize((round(1080 * pw / ph), 1080), Image.LANCZOS)
login.save(f"{RES}/login_art.jpg", quality=90, subsampling=0, optimize=True)
promo.save(f"{RES}/promo_banner.jpg", quality=90, subsampling=0, optimize=True)

for f in sorted(os.listdir(RES)):
    p = os.path.join(RES, f)
    try:
        im = Image.open(p)
        print(f"{f:22s} {im.size} {os.path.getsize(p)//1024} KB")
    except Exception:
        print(f, os.path.getsize(p))
print(splash_layout)
