#!/usr/bin/env python3
"""
Gera o pote de sorvete 3D da Goatilicious (substitui a lata).

Saídas (relativas a frontend/):
  public/assets/models/pote-sorvete.glb      -> modelo com o rótulo "Baunilha & Mel" embutido
  public/assets/textures/pote-mirtilo.webp   -> rótulo "Mirtilo Silvestre" (trocado em runtime)
  scripts/.cache/pote-<sabor>.glb            -> um GLB por sabor (usado só para gerar as miniaturas)

Uso:   python3 scripts/build_pote.py
Requer: numpy, pillow, fonttools (os tipos de letra vêm de node_modules/@fontsource).

O modelo é feito só com geometria de revolução (corpo cónico + tampa), sem ficheiros externos.
Convenção UV do rótulo: u = 0.5 + ângulo/2π (o centro da frente fica em u = 0.5, o verso
em u = 0/1), v = 1 - altura. Por isso a textura de outro sabor pode substituir a embutida.
"""
import json
import math
import struct
import sys
from io import BytesIO
from pathlib import Path

import numpy as np
from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "scripts" / ".cache"
OUT_MODEL = ROOT / "public" / "assets" / "models" / "pote-sorvete.glb"
OUT_TEX = ROOT / "public" / "assets" / "textures" / "pote-mirtilo.webp"

TEX_W, TEX_H = 2048, 688   # ≈ circunferência / altura do corpo
SS = 2                      # supersampling do rótulo
SEGMENTS = 96

FLAVORS = {
    "baunilha": dict(
        nome=("Baunilha", "& Mel"),
        top=(253, 245, 226), bottom=(240, 217, 168),
        ink=(74, 40, 23), accent=(205, 140, 34), accent2=(150, 92, 24),
        rim=(255, 249, 232), lid=(244, 227, 186),
    ),
    "mirtilo": dict(
        nome=("Mirtilo", "Silvestre"),
        top=(226, 208, 246), bottom=(160, 118, 214),
        ink=(45, 15, 87), accent=(84, 32, 148), accent2=(56, 20, 108),
        rim=(247, 240, 255), lid=(84, 32, 148),
    ),
}

# ----------------------------------------------------------------------------- fontes
def font_path(pkg, weight):
    woff = ROOT / "node_modules" / "@fontsource" / pkg / "files" / f"{pkg}-latin-{weight}-normal.woff"
    if not woff.exists():
        sys.exit(f"Tipo de letra não encontrado: {woff} (corra `npm install` primeiro)")
    CACHE.mkdir(parents=True, exist_ok=True)
    ttf = CACHE / f"{pkg}-{weight}.ttf"
    if not ttf.exists():
        f = TTFont(str(woff))
        f.flavor = None
        f.save(str(ttf))
    return str(ttf)


def load_font(pkg, weight, size):
    return ImageFont.truetype(font_path(pkg, weight), int(size * SS))


# ----------------------------------------------------------------------------- rótulo
def s(v):
    return int(round(v * SS))


def text_width(draw, text, font, spacing=0):
    return sum(draw.textlength(ch, font=font) for ch in text) + spacing * SS * max(len(text) - 1, 0)


def draw_text_centered(draw, cx, cy, text, font, fill, spacing=0):
    """Texto centrado em (cx, cy); `spacing` em px do rótulo final (letter-spacing)."""
    w = text_width(draw, text, font, spacing)
    x = s(cx) - w / 2
    for ch in text:
        draw.text((x, s(cy)), ch, font=font, fill=fill, anchor="ls")
        x += draw.textlength(ch, font=font) + spacing * SS


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def hexagon(draw, cx, cy, r, fill, outline):
    pts = [(s(cx + r * math.cos(math.radians(60 * k + 30))), s(cy + r * math.sin(math.radians(60 * k + 30)))) for k in range(6)]
    draw.polygon(pts, fill=fill, outline=outline, width=s(3))


def flower(draw, cx, cy, r, petal, center):
    for k in range(5):
        a = math.radians(72 * k - 90)
        px, py = cx + r * 0.62 * math.cos(a), cy + r * 0.62 * math.sin(a)
        draw.ellipse((s(px - r * 0.5), s(py - r * 0.5), s(px + r * 0.5), s(py + r * 0.5)), fill=petal, outline=(214, 190, 140), width=s(2))
    draw.ellipse((s(cx - r * 0.28), s(cy - r * 0.28), s(cx + r * 0.28), s(cy + r * 0.28)), fill=center)


def berry(draw, cx, cy, r, base, hi, dark):
    draw.ellipse((s(cx - r), s(cy - r), s(cx + r), s(cy + r)), fill=base, outline=dark, width=s(3))
    draw.ellipse((s(cx - r * 0.55), s(cy - r * 0.62), s(cx - r * 0.15), s(cy - r * 0.28)), fill=hi)
    # coroa da frutinha
    for k in range(5):
        a = math.radians(72 * k - 90)
        draw.line((s(cx), s(cy + r * 0.12), s(cx + r * 0.32 * math.cos(a)), s(cy + r * 0.12 + r * 0.32 * math.sin(a))), fill=dark, width=s(3))


def leaf(draw, cx, cy, length, angle, fill, vein):
    a = math.radians(angle)
    w = length * 0.28
    pts = []
    for t in np.linspace(0, 1, 14):
        pts.append((t * length, w * math.sin(math.pi * t)))
    for t in np.linspace(1, 0, 14):
        pts.append((t * length, -w * math.sin(math.pi * t)))
    rot = [(cx + x * math.cos(a) - y * math.sin(a), cy + x * math.sin(a) + y * math.cos(a)) for x, y in pts]
    draw.polygon([(s(x), s(y)) for x, y in rot], fill=fill)
    draw.line((s(cx), s(cy), s(cx + length * math.cos(a)), s(cy + length * math.sin(a))), fill=vein, width=s(2))


def drip(draw, color, shade, y0):
    """Cobertura a escorrer no topo do rótulo (periódica em x, sem costura)."""
    top, bottom = y0, y0 + 36
    for x in range(0, TEX_W, 2):
        f = (46 + 34 * math.sin(2 * math.pi * 3 * x / TEX_W + 1.0)
             + 26 * math.sin(2 * math.pi * 7 * x / TEX_W + 2.1)
             + 16 * math.sin(2 * math.pi * 13 * x / TEX_W + 0.4))
        f = max(f, 18)
        draw.rectangle((s(x), s(top), s(x + 2), s(bottom + f)), fill=color)
    # gotas redondas no fim de alguns escorridos
    for k in range(14):
        x = (k * 2048 / 14) + 40
        f = (46 + 34 * math.sin(2 * math.pi * 3 * x / TEX_W + 1.0)
             + 26 * math.sin(2 * math.pi * 7 * x / TEX_W + 2.1)
             + 16 * math.sin(2 * math.pi * 13 * x / TEX_W + 0.4))
        if f > 70:
            r = 16
            cy = bottom + f
            draw.ellipse((s(x - r), s(cy - r), s(x + r), s(cy + r)), fill=color)
    draw.line((s(0), s(top + 8), s(TEX_W), s(top + 8)), fill=shade, width=s(5))


def make_label(key):
    cfg = FLAVORS[key]
    W, H = TEX_W * SS, TEX_H * SS
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    # fundo em degradê vertical
    for y in range(H):
        d.line((0, y, W, y), fill=lerp(cfg["top"], cfg["bottom"], y / (H - 1)))
    # faixa do rebordo (topo, escondida pela tampa) e base
    d.rectangle((0, 0, W, s(48)), fill=cfg["rim"])
    d.rectangle((0, s(TEX_H * 0.94), W, H), fill=lerp(cfg["bottom"], cfg["accent2"], 0.55))

    cx = TEX_W / 2   # frente
    drip(d, cfg["accent"], cfg["accent2"], 66)

    g_logo = load_font("galada", 400, 112)
    g_name = load_font("galada", 400, 118)
    m_small = load_font("manrope", 800, 25)
    m_tiny = load_font("manrope", 700, 22)

    # logótipo + selo
    draw_text_centered(d, cx, 262, "Goatilicious", g_logo, cfg["ink"])
    d.line((s(cx - 220), s(286), s(cx - 20), s(286)), fill=cfg["accent2"], width=s(3))
    d.line((s(cx + 20), s(286), s(cx + 220), s(286)), fill=cfg["accent2"], width=s(3))
    d.ellipse((s(cx - 7), s(279), s(cx + 7), s(293)), fill=cfg["accent"])
    draw_text_centered(d, cx, 330, "GELADO ARTESANAL DE LEITE DE CABRA", m_small, cfg["ink"], spacing=3)

    # nome do sabor
    draw_text_centered(d, cx, 450, cfg["nome"][0], g_name, cfg["accent"])
    draw_text_centered(d, cx, 566, cfg["nome"][1], g_name, cfg["accent"])
    draw_text_centered(d, cx, 628, "MAPUTO  ·  MOÇAMBIQUE", m_tiny, cfg["ink"], spacing=4)

    # decoração por sabor (dos dois lados do texto)
    if key == "baunilha":
        for side in (-1, 1):
            bx = cx + side * 395
            for dx, dy, r in ((0, 440, 34), (side * 58, 408, 34), (-side * 6, 506, 34), (side * 60, 474, 34), (side * 2, 372, 34)):
                hexagon(d, bx + dx, dy, r, (250, 214, 120), cfg["accent2"])
            flower(d, bx - side * 56, 560, 36, (255, 252, 244), cfg["accent"])
            flower(d, bx + side * 60, 270, 28, (255, 252, 244), cfg["accent"])
    else:
        leafc, vein = (70, 138, 84), (40, 92, 58)
        for side in (-1, 1):
            bx = cx + side * 392
            leaf(d, bx, 500, 90, -125 if side < 0 else -55, leafc, vein)
            leaf(d, bx, 500, 80, -70 if side < 0 else -110, leafc, vein)
            for dx, dy, r in ((0, 470, 42), (side * 54, 520, 38), (-side * 46, 538, 34), (side * 24, 408, 34), (-side * 28, 300, 28), (side * 36, 300, 24)):
                berry(d, bx + dx, dy, r, (58, 42, 150), (140, 128, 230), (28, 18, 90))

    # verso (centro em x=0 e x=2048): dois desenhos para não cortar o texto na costura
    g_back = load_font("galada", 400, 78)
    m_back = load_font("manrope", 700, 22)
    for bx in (0, TEX_W):
        draw_text_centered(d, bx, 300, "Feito à mão", g_back, cfg["ink"])
        draw_text_centered(d, bx, 350, "com leite de cabra fresco,", m_back, cfg["ink"], spacing=1)
        draw_text_centered(d, bx, 384, "sem corantes nem conservantes", m_back, cfg["ink"], spacing=1)
        for dx in (-40, 0, 40):  # três pontinhos (o Manrope não tem o glifo ★)
            d.ellipse((s(bx + dx - 7), s(432), s(bx + dx + 7), s(446)), fill=cfg["accent"])
        draw_text_centered(d, bx, 500, "Conservar a -18 °C", m_back, cfg["ink"], spacing=1)

    return img.resize((TEX_W, TEX_H), Image.LANCZOS)


# ----------------------------------------------------------------------------- geometria
def lathe(polyline, uv_v, double_sided=False):
    """Superfície de revolução. polyline = [(r, y)], percorrida de modo a que a normal
    (dy, -dr) aponte para fora. Devolve (pos, nrm, uv, idx)."""
    pts = np.array(polyline, dtype=np.float64)
    m = len(pts)
    seg = pts[1:] - pts[:-1]
    seg_n = np.stack([seg[:, 1], -seg[:, 0]], axis=1)
    seg_n /= np.linalg.norm(seg_n, axis=1)[:, None]
    vn = np.zeros((m, 2))
    vn[0], vn[-1] = seg_n[0], seg_n[-1]
    for k in range(1, m - 1):
        vn[k] = seg_n[k - 1] + seg_n[k]
    vn /= np.linalg.norm(vn, axis=1)[:, None]

    pos, nrm, uv = [], [], []
    for i in range(SEGMENTS + 1):
        a = -math.pi + 2 * math.pi * i / SEGMENTS
        sa, ca = math.sin(a), math.cos(a)
        for j in range(m):
            r, y = pts[j]
            pos.append((r * sa, y, r * ca))
            nrm.append((vn[j][0] * sa, vn[j][1], vn[j][0] * ca))
            uv.append((0.5 + a / (2 * math.pi), uv_v(y)))
    idx = []
    for i in range(SEGMENTS):
        for j in range(m - 1):
            A, B = i * m + j, (i + 1) * m + j
            C, D = i * m + j + 1, (i + 1) * m + j + 1
            idx += [A, B, D, A, D, C]
    pos = np.array(pos, np.float32)
    nrm = np.array(nrm, np.float32)
    idx = np.array(idx, np.uint32)
    # corrige o enrolamento comparando a normal geométrica com a normal do vértice
    tri = idx.reshape(-1, 3)
    face = np.cross(pos[tri[:, 1]] - pos[tri[:, 0]], pos[tri[:, 2]] - pos[tri[:, 0]])
    ref = nrm[tri].sum(axis=1)
    flip = (face * ref).sum(axis=1) < 0
    tri[flip] = tri[flip][:, [0, 2, 1]]
    return pos, nrm, np.array(uv, np.float32), tri.reshape(-1)


def merge(parts):
    pos, nrm, uv, idx, off = [], [], [], [], 0
    for p, n, u, i in parts:
        pos.append(p); nrm.append(n); uv.append(u); idx.append(i + off); off += len(p)
    return np.concatenate(pos), np.concatenate(nrm), np.concatenate(uv), np.concatenate(idx)


def body_geometry():
    v_of = lambda y: float(np.clip(1.0 - y, 0.0, 1.0))
    base = [(0.0, 0.0), (0.16, 0.0), (0.30, 0.0)]
    wall = [(0.30, 0.0), (0.37, 0.004), (0.405, 0.016), (0.422, 0.040), (0.430, 0.065),
            (0.4735, 0.50), (0.517, 0.93), (0.532, 0.938), (0.540, 0.952), (0.540, 0.985), (0.533, 0.999)]
    return merge([lathe(base, lambda y: 1.0), lathe(wall, v_of)])


def lid_geometry():
    v0 = lambda y: 0.5
    skirt = [(0.545, 0.895), (0.553, 0.905), (0.553, 1.012), (0.545, 1.028), (0.520, 1.040), (0.490, 1.040)]
    under = [(0.50, 0.895), (0.545, 0.895)]
    well = [(0.490, 1.040), (0.468, 1.034), (0.455, 1.012), (0.440, 1.004), (0.0, 1.004)]
    ring = [(0.34, 1.004), (0.345, 1.016), (0.365, 1.022), (0.385, 1.016), (0.39, 1.004)]
    ring2 = [(0.17, 1.004), (0.175, 1.012), (0.19, 1.016), (0.205, 1.012), (0.21, 1.004)]
    return merge([lathe(skirt, v0), lathe(under, v0), lathe(well, v0), lathe(ring, v0), lathe(ring2, v0)])


# ----------------------------------------------------------------------------- GLB
def srgb_to_linear(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def write_glb(path, label_img, lid_rgb):
    bp, bn, bu, bi = body_geometry()
    lp, ln, lu, li = lid_geometry()
    # centra o modelo e normaliza a altura total para ≈ 1
    allp = np.concatenate([bp, lp])
    mn, mx = allp.min(axis=0), allp.max(axis=0)
    centre = (mn + mx) / 2
    scale = 1.0 / (mx[1] - mn[1])
    bp = ((bp - centre) * scale).astype(np.float32)
    lp = ((lp - centre) * scale).astype(np.float32)

    buf = BytesIO()
    views, accs = [], []

    def add(arr, target, comp, typ, with_minmax=False):
        data = arr.tobytes()
        while buf.tell() % 4:
            buf.write(b"\x00")
        views.append(dict(buffer=0, byteOffset=buf.tell(), byteLength=len(data), **({"target": target} if target else {})))
        buf.write(data)
        acc = dict(bufferView=len(views) - 1, componentType=comp, count=len(arr) if arr.ndim == 1 else len(arr), type=typ)
        if with_minmax:
            acc["min"] = arr.min(axis=0).tolist()
            acc["max"] = arr.max(axis=0).tolist()
        accs.append(acc)
        return len(accs) - 1

    prims = []
    for (p, n, u, i, mat) in ((bp, bn, bu, bi, 0), (lp, ln, lu, li, 1)):
        a_i = add(i.astype(np.uint32), 34963, 5125, "SCALAR")
        a_p = add(p, 34962, 5126, "VEC3", True)
        a_n = add(n.astype(np.float32), 34962, 5126, "VEC3")
        attrs = {"POSITION": a_p, "NORMAL": a_n}
        if mat == 0:
            attrs["TEXCOORD_0"] = add(u.astype(np.float32), 34962, 5126, "VEC2")
        prims.append({"attributes": attrs, "indices": a_i, "material": mat, "mode": 4})

    jpg = BytesIO()
    label_img.save(jpg, "JPEG", quality=90, subsampling=0)
    jbytes = jpg.getvalue()
    while buf.tell() % 4:
        buf.write(b"\x00")
    views.append(dict(buffer=0, byteOffset=buf.tell(), byteLength=len(jbytes)))
    buf.write(jbytes)
    img_view = len(views) - 1

    lid_lin = [srgb_to_linear(c) for c in lid_rgb] + [1.0]
    gltf = {
        "asset": {"version": "2.0", "generator": "goatilicious build_pote.py"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"name": "Pote_Sorvete", "mesh": 0}],
        "meshes": [{"name": "Pote", "primitives": prims}],
        "materials": [
            {"name": "Pote_Rotulo", "pbrMetallicRoughness": {"baseColorTexture": {"index": 0}, "metallicFactor": 0.0, "roughnessFactor": 0.42}},
            {"name": "Pote_Tampa", "doubleSided": True, "pbrMetallicRoughness": {"baseColorFactor": lid_lin, "metallicFactor": 0.0, "roughnessFactor": 0.5}},
        ],
        "textures": [{"sampler": 0, "source": 0}],
        "samplers": [{"magFilter": 9729, "minFilter": 9987, "wrapS": 10497, "wrapT": 33071}],
        "images": [{"bufferView": img_view, "mimeType": "image/jpeg", "name": "rotulo"}],
        "bufferViews": views,
        "accessors": accs,
        "buffers": [{"byteLength": buf.tell()}],
    }
    js = json.dumps(gltf, separators=(",", ":")).encode()
    js += b" " * (-len(js) % 4)
    bin_ = buf.getvalue()
    bin_ += b"\x00" * (-len(bin_) % 4)
    total = 12 + 8 + len(js) + 8 + len(bin_)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as f:
        f.write(struct.pack("<III", 0x46546C67, 2, total))
        f.write(struct.pack("<II", len(js), 0x4E4F534A) + js)
        f.write(struct.pack("<II", len(bin_), 0x004E4942) + bin_)
    return total


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    for key, cfg in FLAVORS.items():
        label = make_label(key)
        label.save(CACHE / f"rotulo-{key}.png")
        size = write_glb(CACHE / f"pote-{key}.glb", label, cfg["lid"])
        print(f"{key}: GLB {size/1024:.0f} KB")
        if key == "baunilha":
            write_glb(OUT_MODEL, label, cfg["lid"])
        else:
            OUT_TEX.parent.mkdir(parents=True, exist_ok=True)
            label.save(OUT_TEX, "WEBP", quality=88, method=6)
    print("OK ->", OUT_MODEL.relative_to(ROOT), "|", OUT_TEX.relative_to(ROOT))


if __name__ == "__main__":
    main()
