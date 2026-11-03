"""Fondos de Lumina TV: ondas de luz rosa y violeta (1920x1080).

  bg_home     ondas diagonales magenta (inicio, login, ajustes, búsqueda)
  art_live    ondas violeta + arcos de señal (TV en vivo y guía)
  art_movies  haz de proyector de cine con polvo de luz (películas)
  art_series  ondas índigo con bokeh (series)
"""
import os
import numpy as np
import cv2
from PIL import Image

W, H = 1920, 1080
OUT = "res"
os.makedirs(OUT, exist_ok=True)
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)


def base(c_center, c_edge, cx=0.5, cy=0.5, r=0.9):
    d = np.sqrt(((xx / W - cx) * 1.6) ** 2 + ((yy / H - cy)) ** 2) / r
    t = np.clip(d, 0, 1) ** 1.2
    return (np.array(c_center, np.float32)[None, None] * (1 - t[..., None])
            + np.array(c_edge, np.float32)[None, None] * t[..., None])


def ribbons(n, y0, amp, freq, phase, spread, tilt, thick, rng, x_from=-0.1, x_to=1.1):
    """Haz de muchas curvas finas casi paralelas: parece seda hecha de luz."""
    layer = np.zeros((H, W), np.float32)
    xs = np.linspace(x_from * W, x_to * W, 900)
    u = (xs / W)
    for k in range(n):
        f = k / max(1, n - 1)
        ph = phase + f * spread
        y = (y0 + tilt * (u - 0.5)) * H \
            + amp * H * np.sin(u * np.pi * freq + ph) \
            + amp * 0.35 * H * np.sin(u * np.pi * freq * 2.3 + ph * 1.7 + 1.3) \
            + (f - 0.5) * thick * H
        pts = np.stack([xs, y], 1).astype(np.int32).reshape(-1, 1, 2)
        a = 0.10 + 0.9 * np.exp(-((f - 0.5) / 0.28) ** 2)   # el centro del haz brilla más
        cv2.polylines(layer, [pts], False, float(a), 1, cv2.LINE_AA)
    return layer


def colorize(layer, c1, c2, direction="x"):
    t = (xx / W) if direction == "x" else (yy / H)
    col = (np.array(c1, np.float32)[None, None] * (1 - t[..., None])
           + np.array(c2, np.float32)[None, None] * t[..., None])
    return col * layer[..., None]


def glow_stack(layer):
    """Brillo suave a varias distancias (luz que 'sangra')."""
    out = layer * 0.55
    for s, w in ((2, 0.6), (8, 0.55), (26, 0.5), (70, 0.45)):
        out = out + cv2.GaussianBlur(layer, (0, 0), s) * w * (s / 2) ** 0.35
    return out


def particles(n, rng, region=None, size=(0.6, 2.4), alpha=(0.2, 0.9)):
    layer = np.zeros((H, W), np.float32)
    for _ in range(n):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        if region is not None and not region(x / W, y / H):
            continue
        r = rng.uniform(*size)
        cv2.circle(layer, (int(x), int(y)), max(1, int(round(r))), float(rng.uniform(*alpha)), -1, cv2.LINE_AA)
    return cv2.GaussianBlur(layer, (0, 0), 0.8) + cv2.GaussianBlur(layer, (0, 0), 4) * 0.8


def bokeh(n, rng, rmin, rmax, region=None):
    layer = np.zeros((H, W), np.float32)
    for _ in range(n):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        if region is not None and not region(x / W, y / H):
            continue
        r = rng.uniform(rmin, rmax)
        a = rng.uniform(0.05, 0.22)
        disc = np.zeros_like(layer)
        cv2.circle(disc, (int(x), int(y)), int(r), a, -1, cv2.LINE_AA)
        ring = np.zeros_like(layer)
        cv2.circle(ring, (int(x), int(y)), int(r), a * 0.9, 2, cv2.LINE_AA)
        layer = np.maximum(layer, cv2.GaussianBlur(disc + ring, (0, 0), 1.5))
    return layer


def soft_glow(cx, cy, rx, ry, color, a):
    g = np.exp(-(((xx / W - cx) / rx) ** 2 + ((yy / H - cy) / ry) ** 2))
    return np.array(color, np.float32)[None, None] * (g * a)[..., None]


def finish(img, rng, name, vignette=0.5):
    v = np.sqrt((xx / W - 0.5) ** 2 + (yy / H - 0.5) ** 2) / 0.72
    img = img * (1 - vignette * np.clip(v, 0, 1) ** 1.8)[..., None]
    # tono: comprime luces para que no se queme el blanco
    img = 255 * (1 - np.exp(-img / 255 * 1.25)) / (1 - np.exp(-1.25))
    img = img + rng.uniform(-1.4, 1.4, img.shape)  # tramado contra escalones
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")
    out.save(f"{OUT}/{name}.jpg", quality=92, subsampling=0, optimize=True)
    return out


# ------------------------------------------------------------------ inicio
rng = np.random.default_rng(21)
img = base([26, 6, 30], [7, 1, 10], cx=0.7, cy=0.45)
img += soft_glow(0.82, 0.30, 0.45, 0.45, [120, 16, 82], 0.55)
r1 = ribbons(70, 0.62, 0.07, 1.3, 0.4, 1.1, -0.55, 0.10, rng)
r2 = ribbons(45, 0.70, 0.05, 1.7, 2.2, 0.8, -0.45, 0.07, rng)
img += colorize(glow_stack(r1), [255, 70, 160], [190, 90, 255]) * 0.95
img += colorize(glow_stack(r2), [140, 60, 255], [255, 110, 200]) * 0.55
img += colorize(particles(220, rng, lambda x, y: x > 0.35), [255, 170, 220], [255, 200, 240]) * 0.9
finish(img, rng, "bg_home")

# ------------------------------------------------------------------ TV en vivo
rng = np.random.default_rng(22)
img = base([20, 6, 34], [6, 1, 12], cx=0.2, cy=0.9)
img += soft_glow(0.05, 1.0, 0.50, 0.50, [110, 30, 140], 0.45)
# arcos de señal desde abajo a la izquierda
d = np.sqrt((xx - 0.02 * W) ** 2 + (yy - 1.02 * H) ** 2)
arcs = np.zeros((H, W), np.float32)
for i, rad in enumerate([0.32, 0.48, 0.66, 0.86, 1.08]):
    arcs += np.exp(-((d - rad * H) / 2.2) ** 2) * (0.75 - i * 0.11)
arcs = arcs * np.clip((xx / W) * 3, 0, 1)
img += colorize(glow_stack(arcs), [255, 80, 180], [150, 90, 255]) * 0.38
r1 = ribbons(60, 0.30, 0.06, 1.2, 1.5, 1.0, 0.35, 0.09, rng)
r1 = r1 * np.clip((xx / W - 0.12) / 0.35, 0, 1) ** 1.5      # la onda nace suave, sin corte
img += colorize(glow_stack(r1), [170, 80, 255], [255, 80, 170]) * 0.65
img += colorize(particles(160, rng, lambda x, y: x > 0.4), [210, 170, 255], [255, 190, 230]) * 0.8
finish(img, rng, "art_live")

# ------------------------------------------------------------------ películas: haz de proyector
rng = np.random.default_rng(23)
img = base([24, 5, 18], [7, 1, 9], cx=0.75, cy=0.25)
src = np.array([1.02 * W, -0.06 * H])
vx, vy = xx - src[0], yy - src[1]
ang = np.degrees(np.arctan2(vy, vx))       # dirección del haz: hacia abajo-izquierda (~145°)
dist = np.sqrt(vx ** 2 + vy ** 2) / W
cone = np.exp(-((ang - 142) / 13) ** 2) * np.exp(-dist * 1.1)
rays = np.zeros_like(cone)
for _ in range(9):                          # rayos finos dentro del haz
    a0, w0 = rng.uniform(132, 152), rng.uniform(0.6, 2.2)
    rays += np.exp(-((ang - a0) / w0) ** 2) * rng.uniform(0.15, 0.45) * np.exp(-dist * 1.3)
beam = cone * 0.9 + rays
img += colorize(cv2.GaussianBlur(beam, (0, 0), 3), [255, 120, 190], [255, 160, 210], "y") * 0.85
img += soft_glow(1.0, 0.0, 0.10, 0.16, [255, 210, 235], 0.9)
dust = particles(420, rng, lambda x, y: True, size=(0.5, 1.8), alpha=(0.25, 1.0))
img += colorize(dust * cv2.GaussianBlur(beam, (0, 0), 20) * 2.2, [255, 220, 240], [255, 200, 230]) * 1.0
r1 = ribbons(40, 0.86, 0.035, 1.4, 0.8, 0.7, -0.1, 0.06, rng)
img += colorize(glow_stack(r1), [255, 60, 140], [180, 60, 220]) * 0.45
finish(img, rng, "art_movies")

# ------------------------------------------------------------------ series: ondas índigo + bokeh
rng = np.random.default_rng(24)
img = base([16, 8, 38], [5, 2, 12], cx=0.5, cy=0.15)
img += soft_glow(0.5, 0.0, 0.6, 0.35, [70, 30, 150], 0.55)
r1 = ribbons(65, 0.20, 0.05, 1.0, 3.1, 1.2, 0.05, 0.10, rng)
r2 = ribbons(40, 0.88, 0.04, 1.6, 0.3, 0.9, -0.05, 0.07, rng)
img += colorize(glow_stack(r1), [110, 90, 255], [255, 80, 190]) * 0.75
img += colorize(glow_stack(r2), [255, 80, 180], [120, 90, 255]) * 0.45
img += colorize(bokeh(26, rng, 14, 60, lambda x, y: (y < 0.35 or y > 0.75)), [180, 140, 255], [255, 150, 220]) * 1.0
img += colorize(particles(140, rng), [200, 180, 255], [255, 200, 240]) * 0.6
finish(img, rng, "art_series")
print("ok")
