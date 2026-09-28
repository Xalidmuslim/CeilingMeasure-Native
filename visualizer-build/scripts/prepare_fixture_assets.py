from __future__ import annotations

import io
import os
import urllib.request
from pathlib import Path
from statistics import median
from PIL import Image, ImageDraw, ImageFont

MAIN_URL = "https://cdn.openart.ai/watermarked_images/wwKL72mZPzCRR9UDPBp1/thumbnail_8678e3e8_1790620789465.webp"

OUT = Path("visualizer-build/generated-fixtures")
OUT.mkdir(parents=True, exist_ok=True)

def fetch(url: str) -> Image.Image:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 CeilingVisualizer/0.8"})
    with urllib.request.urlopen(req, timeout=40) as r:
        data = r.read()
    return Image.open(io.BytesIO(data)).convert("RGBA")

def bg_color(cell: Image.Image):
    w, h = cell.size
    pts = []
    step = max(2, min(w, h) // 80)
    for x in range(0, w, step):
        for y in (0, min(h - 1, step), max(0, h - 1 - step), h - 1):
            pts.append(cell.getpixel((x, y))[:3])
    for y in range(0, h, step):
        for x in (0, min(w - 1, step), max(0, w - 1 - step), w - 1):
            pts.append(cell.getpixel((x, y))[:3])
    return tuple(int(median([p[i] for p in pts])) for i in range(3))

def key_green(cell: Image.Image) -> Image.Image:
    return key_background(cell, mode="green")

def key_magenta(cell: Image.Image) -> Image.Image:
    return key_background(cell, mode="magenta")

def key_background(cell: Image.Image, mode: str) -> Image.Image:
    img = cell.copy()
    pix = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pix[x, y]
            if mode == "green":
                dominance = g - max(r, b)
                strength = g
            else:
                dominance = min(r, b) - g
                strength = min(r, b)

            if strength > 155 and dominance > 38:
                if strength > 205 and dominance > 90:
                    na = 0
                else:
                    na = max(0, min(255, int(255 - (dominance - 38) * 4.7)))
            else:
                na = 255

            if na < 255:
                if mode == "green":
                    g = min(g, max(r, b) + 8)
                else:
                    neutral = g + 8
                    r = min(r, neutral)
                    b = min(b, neutral)
            pix[x, y] = (r, g, b, min(a, na))

    alpha = img.getchannel("A")
    bbox = alpha.getbbox()
    if bbox:
        l, t, r, b = bbox
        pad = 12
        img = img.crop((max(0,l-pad), max(0,t-pad), min(w,r+pad), min(h,b+pad)))
    return img

def neutralize_green_foreground(img: Image.Image) -> Image.Image:
    out = img.copy()
    pix = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pix[x, y]
            if a <= 16:
                continue
            mx = max(r, b)
            if g > mx + 14:
                if mx < 150:
                    v = max(12, min(115, int((r + b) * 0.48)))
                    pix[x, y] = (v, v, v, a)
                else:
                    pix[x, y] = (r, min(255, mx + 4), b, a)
    return out

def crop_cell(sheet: Image.Image, cols: int, rows: int, col: int, row: int):
    cw = sheet.width // cols
    ch = sheet.height // rows
    left, top = col * cw, row * ch
    right = sheet.width if col == cols - 1 else (col + 1) * cw
    bottom = sheet.height if row == rows - 1 else (row + 1) * ch
    return sheet.crop((left, top, right, bottom)).convert("RGBA")

main = fetch(MAIN_URL)

selection = [
    ("spot_white", 0, 0),
    ("spot_black", 1, 0),
    ("ring_black", 0, 3),
    ("three_globes", 3, 3),
]

assets = []
for name, col, row in selection:
    cell = crop_cell(main, 4, 4, col, row)
    keyed = neutralize_green_foreground(key_green(cell))
    keyed.thumbnail((480, 480), Image.Resampling.LANCZOS)
    path = OUT / f"{name}.png"
    keyed.save(path, "PNG", optimize=True)
    assets.append((name, keyed))

# QA preview on checkerboard. This is what gets inspected before assets are accepted.
tile_w, tile_h = 420, 500
preview = Image.new("RGB", (tile_w * 3, tile_h * 2), (82, 82, 82))
draw = ImageDraw.Draw(preview)
checker = 28
for y in range(0, preview.height, checker):
    for x in range(0, preview.width, checker):
        if ((x // checker) + (y // checker)) % 2:
            draw.rectangle((x, y, x + checker - 1, y + checker - 1), fill=(120, 120, 120))

for idx, (name, img) in enumerate(assets):
    col, row = idx % 3, idx // 3
    box_x, box_y = col * tile_w, row * tile_h
    work = img.copy()
    work.thumbnail((330, 385), Image.Resampling.LANCZOS)
    x = box_x + (tile_w - work.width) // 2
    y = box_y + 45 + (390 - work.height) // 2
    preview.paste(work, (x, y), work)
    draw.rectangle((box_x + 8, box_y + 8, box_x + tile_w - 8, box_y + 38), fill=(25, 25, 25))
    draw.text((box_x + 18, box_y + 14), name, fill=(245, 245, 245))

preview.save(OUT / "qa_preview.png", "PNG", optimize=True)
print("Prepared:", ", ".join(n for n, _ in assets))
