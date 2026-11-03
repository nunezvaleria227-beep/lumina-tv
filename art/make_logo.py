"""Reconstruye el logo de Lumina TV en alta resolucion a partir del PNG del usuario.

Las letras blancas y la pastilla rosa "TV" se separan por color, se amplian con
interpolacion cubica y se vuelven a cortar con un borde suave, asi quedan nitidas
a 4K. La estrella se dibuja como vector (forma de destello de 4 puntas).

Salidas (carpeta out/):
  wordmark_master.png  LUMINA TV sin estrella, RGBA, escala x4 del original
  star_master.png      estrella con brillo, RGBA
  logo_master.png      logo completo (palabra + estrella)
  layout.json          posicion de la estrella respecto a la palabra
"""
import json, os
import numpy as np
import cv2
from PIL import Image

OUT = "out"
os.makedirs(OUT, exist_ok=True)
S = 4  # factor de ampliacion del master

src = np.asarray(Image.open("logo_src.png").convert("RGB")).astype(np.float32) / 255.0

# Zona del logo en el original (con margen)
X0, X1, Y0, Y1 = 120, 1140, 560, 708
PILL_X = 900       # a la derecha: pastilla rosa "TV"
STAR_CUT = 574     # arriba de esta linea solo esta la estrella

reg = src[Y0:Y1, X0:X1]
h, w, _ = reg.shape


def up(a):
    return cv2.resize(a, (w * S, h * S), interpolation=cv2.INTER_CUBIC)


def smooth(x, lo, hi):
    t = np.clip((x - lo) / (hi - lo), 0, 1)
    return t * t * (3 - 2 * t)


xs = np.arange(w)[None, :] + X0
ys = np.arange(h)[:, None] + Y0
left = (xs < PILL_X).astype(np.float32) * (ys >= STAR_CUT)
right = (xs >= PILL_X).astype(np.float32) * np.ones_like(ys, dtype=np.float32)

# --- letras blancas: canal minimo alto = blanco
mn = cv2.GaussianBlur(reg.min(axis=2), (0, 0), 0.6) * left
white = smooth(up(mn), 0.40, 0.64)
white = smooth(cv2.GaussianBlur(white, (0, 0), 2.6), 0.40, 0.60)

# --- pastilla y letras TV: canal rojo alto
rr = cv2.GaussianBlur(reg[..., 0], (0, 0), 0.6) * right
pink = smooth(up(rr), 0.48, 0.68)
pink = smooth(cv2.GaussianBlur(pink, (0, 0), 2.2), 0.40, 0.60)

# Color de la parte rosa sin el halo oscuro del borde (convolucion normalizada)
wmask = smooth(rr, 0.62, 0.80)
num = np.stack([cv2.GaussianBlur(reg[..., c] * wmask, (0, 0), 1.6) for c in range(3)], -1)
den = cv2.GaussianBlur(wmask, (0, 0), 1.6)[..., None] + 1e-4
pink_col = np.clip(up(num / den), 0, 1)
# un poco mas de saturacion y luz
hsv = cv2.cvtColor((pink_col * 255).astype(np.uint8), cv2.COLOR_RGB2HSV).astype(np.float32)
hsv[..., 1] = np.clip(hsv[..., 1] * 1.08, 0, 255)
hsv[..., 2] = np.clip(hsv[..., 2] * 1.03, 0, 255)
pink_col = cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2RGB).astype(np.float32) / 255

H2, W2 = white.shape
# Relleno blanco con un degradado muy sutil (blanco arriba, blanco rosado abajo)
yy = np.linspace(0, 1, H2)[:, None, None]
white_col = (1 - yy) * np.array([1.0, 1.0, 1.0]) + yy * np.array([0.97, 0.91, 0.96])
white_col = np.broadcast_to(white_col, (H2, W2, 3))

# Lienzo con margen para brillo y sombra
PAD = 140
CH, CW = H2 + 2 * PAD, W2 + 2 * PAD


def padded(a):
    out = np.zeros((CH, CW) + a.shape[2:], np.float32)
    out[PAD:PAD + H2, PAD:PAD + W2] = a
    return out


A_w = padded(white)
A_p = padded(pink)
C_w = padded(np.ascontiguousarray(white_col, dtype=np.float32))
C_p = padded(pink_col)
A_all = np.clip(A_w + A_p, 0, 1)


def over(dst_rgb, dst_a, src_rgb, src_a):
    a = src_a + dst_a * (1 - src_a)
    rgb = (src_rgb * src_a[..., None] + dst_rgb * dst_a[..., None] * (1 - src_a[..., None])) / (a[..., None] + 1e-6)
    return rgb, a


rgb = np.zeros((CH, CW, 3), np.float32)
alpha = np.zeros((CH, CW), np.float32)

# 1) brillo rosa amplio
glow = cv2.GaussianBlur(A_all, (0, 0), 46) * 0.55
rgb, alpha = over(rgb, alpha, np.broadcast_to(np.array([1.0, 0.18, 0.58], np.float32), (CH, CW, 3)), glow)
# 2) sombra suave hacia abajo-derecha (profundidad)
M = np.float32([[1, 0, 10], [0, 1, 16]])
sh = cv2.warpAffine(A_all, M, (CW, CH))
sh = cv2.GaussianBlur(sh, (0, 0), 9) * 0.70
rgb, alpha = over(rgb, alpha, np.zeros((CH, CW, 3), np.float32), sh)
# 3) halo cercano rosado alrededor de las letras blancas
near = cv2.GaussianBlur(A_w, (0, 0), 7) * 0.45
rgb, alpha = over(rgb, alpha, np.broadcast_to(np.array([1.0, 0.45, 0.78], np.float32), (CH, CW, 3)), near)
# 4) rellenos
rgb, alpha = over(rgb, alpha, C_p, A_p)
rgb, alpha = over(rgb, alpha, C_w, A_w)


def save_rgba(path, rgb, a):
    arr = np.dstack([np.clip(rgb, 0, 1), np.clip(a, 0, 1)])
    Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA").save(path, optimize=True)


save_rgba(f"{OUT}/wordmark_master.png", rgb, alpha)

# --- estrella vectorial ----------------------------------------------------
# Centro de la estrella en el original: el pixel mas brillante de su zona
sz = src[495:575, 530:640].sum(axis=2)
sy, sx = np.unravel_index(np.argmax(cv2.GaussianBlur(sz, (0, 0), 2)), sz.shape)
star_cx_src, star_cy_src = 530 + sx, 495 + sy
# Brazos medidos en el original (px): horizontal y vertical
ARM_H, ARM_UP, ARM_DN = 41, 38, 33


def star_shape(size, cx, cy, ah, au, ad, p=4.6):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    dx = (xx - cx) / ah
    dy = np.where(yy < cy, (yy - cy) / au, (yy - cy) / ad)
    # forma de "astroide": |x|^(2/p) + |y|^(2/p) <= 1  -> lados curvos hacia adentro
    v = np.abs(dx) ** (2 / p) + np.abs(dy) ** (2 / p)
    return v


SS = 900  # lienzo de la estrella (en escala master)
c = SS / 2
v = star_shape(SS, c, c, ARM_H * S, ARM_UP * S, ARM_DN * S)
# borde suave ~1.5px
edge = 1.5 / (ARM_H * S) * 2.2
star_a = smooth(1 - v, -edge, edge)
r = np.hypot(*(np.mgrid[0:SS, 0:SS] - c)) / (ARM_H * S)
# color: nucleo blanco -> rosa -> magenta en las puntas
core = np.exp(-(r / 0.10) ** 2)
mid = np.exp(-(r / 0.30) ** 2)
star_col = (np.array([0.88, 0.16, 0.52])[None, None] * (1 - mid[..., None])
            + np.array([1.0, 0.55, 0.82])[None, None] * mid[..., None])
star_col = star_col * (1 - core[..., None]) + np.array([1.0, 0.97, 1.0])[None, None] * core[..., None]

srgb = np.zeros((SS, SS, 3), np.float32)
sa = np.zeros((SS, SS), np.float32)
g1 = np.exp(-(r / 0.95) ** 2) * 0.50            # brillo amplio
srgb, sa = over(srgb, sa, np.broadcast_to(np.array([1.0, 0.16, 0.58], np.float32), (SS, SS, 3)), g1)
g2 = np.exp(-(r / 0.30) ** 2) * 0.75            # brillo del centro
srgb, sa = over(srgb, sa, np.broadcast_to(np.array([1.0, 0.55, 0.85], np.float32), (SS, SS, 3)), g2)
srgb, sa = over(srgb, sa, star_col.astype(np.float32), star_a.astype(np.float32))
# destello fino horizontal
streak = np.exp(-((np.mgrid[0:SS, 0:SS][0] - c) / 3.0) ** 2) * np.exp(-(np.abs(np.mgrid[0:SS, 0:SS][1] - c) / (ARM_H * S * 1.6)) ** 1.2) * 0.0
srgb, sa = over(srgb, sa, np.broadcast_to(np.array([1.0, 0.8, 0.95], np.float32), (SS, SS, 3)), streak.astype(np.float32))
save_rgba(f"{OUT}/star_master.png", srgb, sa)

# --- logo completo -----------------------------------------------------------
star_cx = PAD + (star_cx_src - X0) * S
star_cy = PAD + (star_cy_src - Y0) * S
# ampliar el lienzo hacia arriba para que quepa la estrella con su brillo
top_extra = int(max(0, SS / 2 - star_cy)) + 10
FH, FW = CH + top_extra, CW
frgb = np.zeros((FH, FW, 3), np.float32)
fa = np.zeros((FH, FW), np.float32)
frgb[top_extra:], fa[top_extra:] = rgb, alpha
# pegar la estrella
sy0, sx0 = int(star_cy + top_extra - c), int(star_cx - c)
crop_rgb = frgb[sy0:sy0 + SS, sx0:sx0 + SS]
crop_a = fa[sy0:sy0 + SS, sx0:sx0 + SS]
nr, na = over(crop_rgb, crop_a, srgb, sa)
frgb[sy0:sy0 + SS, sx0:sx0 + SS], fa[sy0:sy0 + SS, sx0:sx0 + SS] = nr, na
save_rgba(f"{OUT}/logo_master.png", frgb, fa)

layout = {
    "wordmark_size": [CW, CH],
    "star_size": SS,
    "star_center_in_wordmark": [float(star_cx), float(star_cy)],
    "star_arm_px": ARM_H * S,
    "logo_size": [FW, FH],
    "star_src": [int(star_cx_src), int(star_cy_src)],
}
json.dump(layout, open(f"{OUT}/layout.json", "w"), indent=1)
print(layout)
