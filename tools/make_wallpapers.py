#!/usr/bin/env python3
"""Draws the Bhamley Glass wallpapers: soft liquid light, silk ribbons and glass orbs.

Everything is generated in code (no photos, no third-party art), so the pictures are
CC0 and reproducible: same seed, same pixels.

    python3 tools/make_wallpapers.py            # writes themes/bhamley-glass/wallpapers/*.jpg

Needs numpy, scipy and Pillow. The three files match wallpaper_moods in config.json:
morning (sunrise to noon), evening (until just after sunset) and night.

Layout rules the pictures follow, so the launcher UI stays readable on top of them:
  * the top 12% is kept calm and dark (Wi-Fi, Settings and the clock sit there);
  * the bottom 40% fades to near black (the dock and the app grid sit there);
  * orbs and ribbons live in the upper two thirds, mostly off the centre line; the top-left corner, where the
    date and time sit, holds light but no orb, and the large orbs sit behind where frosted panels will be;
  * dithering noise is added before the 8-bit conversion so dark gradients never band.
"""
import io
import os
import sys

import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

W, H = 1920, 1080
MAX_KB = 299  # the launcher project's theme recipe keeps every picture under 300 KB
OUT =os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "themes", "bhamley-glass", "wallpapers")

Y, X = np.mgrid[0:H, 0:W].astype(np.float32)


def rgb(hex_color):
    h = hex_color.lstrip("#")
    return np.array([int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)], dtype=np.float32)


def vertical_gradient(top, bottom):
    t = (Y / (H - 1))[..., None]
    return rgb(top) * (1 - t) + rgb(bottom) * t


def blob(img, cx, cy, rx, ry, color, strength):
    """A soft elliptical light, added to the picture."""
    g = np.exp(-(((X - cx) / rx) ** 2 + ((Y - cy) / ry) ** 2) / 2.0)
    img += g[..., None] * rgb(color) * strength


def ribbon(img, y0, amp1, wl1, ph1, amp2, wl2, ph2, thick, c_left, c_right, strength, blur=6.0):
    """A silky band that follows two sine waves, shaded from one colour to another along x."""
    yc = y0 + amp1 * np.sin(2 * np.pi * X / wl1 + ph1) + amp2 * np.sin(2 * np.pi * X / wl2 + ph2)
    core = np.exp(-(((Y - yc) / thick) ** 2))
    # a bright thin crest on top of the soft body gives the "liquid" edge
    crest = np.exp(-(((Y - (yc - thick * 0.35)) / (thick * 0.18)) ** 2))
    a = gaussian_filter(core * 0.7 + crest * 0.45, blur)
    t = (X / (W - 1))[..., None]
    col = rgb(c_left) * (1 - t) + rgb(c_right) * t
    img += a[..., None] * col * strength


def orb(img, cx, cy, r, tint, strength=1.0):
    """A glass sphere: darker body, bright rim lit from the upper left, caustic lower right."""
    dx = (X - cx) / r
    dy = (Y - cy) / r
    d = np.sqrt(dx * dx + dy * dy)
    mask = np.clip((1.0 - d) * r, 0.0, 1.0)  # one-pixel anti-aliased edge
    dc = np.clip(d, 0.0, 1.0)
    ndot = dx * -0.6 + dy * -0.8
    body = 0.05 + 0.26 * dc ** 3
    rim = np.clip(ndot, 0, 1) ** 2 * dc ** 6 * 0.95
    caustic = np.clip(-ndot, 0, 1) ** 2 * dc ** 4 * 0.55
    spec = np.exp(-(((dx + 0.36) / 0.20) ** 2 + ((dy + 0.46) / 0.10) ** 2)) * 0.6
    t = rgb(tint)
    white = np.array([1.0, 1.0, 1.0], dtype=np.float32)
    val = (body[..., None] * t + rim[..., None] * (0.55 * white + 0.45 * t)
           + caustic[..., None] * (0.35 * white + 0.65 * t) + spec[..., None] * white)
    img += val * mask[..., None] * strength
    # soft halo outside the sphere
    halo = np.exp(-np.clip(d - 1.0, 0, None) * 3.2) * (d > 1.0) * 0.10
    img += halo[..., None] * t * strength


def finish(img, seed, bottom_fade=0.40, top_calm=0.12):
    # keep the top calm and the bottom dark so pills, clock, dock and tiles stay readable
    t = (Y / (H - 1)).astype(np.float32)
    top = np.clip(1.0 - np.clip((top_calm - t) / top_calm, 0, 1) * 0.35, 0, 1)
    bot = 1.0 - np.clip((t - (1.0 - bottom_fade)) / bottom_fade, 0, 1) ** 1.4 * 0.82
    vig = 1.0 - 0.28 * (((X - W / 2) / (W / 2)) ** 2 + ((Y - H / 2) / (H / 2)) ** 2) / 2.0
    img *= (top * bot * vig)[..., None]
    img = np.clip(img, 0, 1) ** (1 / 1.04)
    rng = np.random.default_rng(seed)
    img += rng.normal(0.0, 1.1 / 255.0, img.shape).astype(np.float32)  # dither against banding
    return (np.clip(img, 0, 1) * 255.0 + 0.5).astype(np.uint8)


def morning():
    img = vertical_gradient("#0e1a2c", "#080d17")
    blob(img, 1520, 270, 440, 280, "#ff9a68", 0.36)   # peach sun glow, upper right
    blob(img, 300, 330, 470, 320, "#2fc4b8", 0.28)    # teal, upper left
    # teal and peach add up to grey where they meet, so a lavender bridge sits between them
    blob(img, 900, 260, 380, 260, "#9a8cff", 0.17)
    ribbon(img, 470, 70, 1500, 0.4, 28, 640, 1.8, 62, "#2fc4b8", "#ffb38a", 0.30)
    ribbon(img, 560, 55, 1200, 2.1, 22, 520, 0.6, 38, "#7fe3d6", "#ffd9a8", 0.20)
    orb(img, 1560, 300, 150, "#ffc9a3", 0.95)
    orb(img, 1270, 205, 56, "#9ff0e4", 0.85)
    orb(img, 470, 548, 94, "#7fe3d6", 0.85)
    orb(img, 1740, 520, 40, "#ffe0bd", 0.7)
    return finish(img, seed=11)


def evening():
    img = vertical_gradient("#150f2e", "#090611")
    blob(img, 1400, 300, 600, 340, "#e8508f", 0.36)   # magenta
    blob(img, 420, 280, 560, 320, "#6a52e6", 0.34)    # violet
    blob(img, 960, 470, 760, 120, "#ff9a3d", 0.16)    # amber horizon
    ribbon(img, 480, 80, 1400, 1.1, 30, 600, 0.2, 66, "#6a52e6", "#e8508f", 0.32)
    ribbon(img, 575, 50, 1100, 3.0, 20, 480, 2.2, 36, "#ff9a3d", "#ff6fb0", 0.22)
    orb(img, 1480, 285, 142, "#ff7fb8", 0.95)
    orb(img, 1215, 180, 52, "#ffb072", 0.85)
    orb(img, 470, 548, 100, "#9a86ff", 0.9)
    orb(img, 1730, 505, 42, "#ffc5e0", 0.7)
    return finish(img, seed=23)


def night():
    img = vertical_gradient("#0a1124", "#05070d")
    blob(img, 1450, 280, 560, 320, "#6c52e0", 0.32)   # violet
    blob(img, 360, 300, 600, 340, "#1fa6c0", 0.30)    # teal
    blob(img, 960, 480, 820, 110, "#2a5bd6", 0.14)    # deep blue horizon
    ribbon(img, 475, 75, 1450, 0.9, 26, 620, 1.4, 64, "#1fa6c0", "#6c52e0", 0.30)
    ribbon(img, 565, 52, 1150, 2.6, 20, 500, 0.9, 36, "#4aa8ff", "#9a86ff", 0.20)
    orb(img, 1520, 290, 146, "#8f78ff", 0.95)
    orb(img, 1240, 195, 54, "#5ecbff", 0.85)
    orb(img, 470, 548, 98, "#35c9d6", 0.88)
    orb(img, 1745, 515, 40, "#b9a8ff", 0.7)
    return finish(img, seed=37)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("morning", morning), ("evening", evening), ("night", night)):
        pixels = fn()
        path = os.path.join(OUT, name + ".jpg")
        data, quality = encode(pixels)
        with open(path, "wb") as fh:
            fh.write(data)
        print("wrote", os.path.normpath(path), "%d KB at JPEG quality %d" % (len(data) // 1024, quality))
    return 0


def encode(pixels):
    """The highest JPEG quality that stays under MAX_KB.

    Not the usual 90 to 92: there the quantiser erases the dither noise and the dark gradients turn into faint
    8x8 blocks (scripts/validate.py measures it). The launcher project's theme recipe keeps a picture under
    300 KB, and quality 95 to 96 meets both: the noise survives and the file stays small.
    """
    for quality in range(97, 90, -1):
        buf = io.BytesIO()
        Image.fromarray(pixels, "RGB").save(buf, "JPEG", quality=quality, subsampling=0, optimize=True)
        if len(buf.getvalue()) <= MAX_KB * 1024:
            return buf.getvalue(), quality
    return buf.getvalue(), quality


if __name__ == "__main__":
    sys.exit(main())
