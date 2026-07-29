"""Generate Google Play listing assets for Skry."""

from __future__ import annotations

import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))
BG = (13, 13, 13, 255)
PRIMARY = (99, 102, 241, 255)
PRIMARY_V = (139, 92, 246, 255)
WHITE = (248, 250, 252, 255)
MUTED = (148, 163, 184, 255)


def cubic(p0, p1, p2, p3, t: float):
    x = (
        (1 - t) ** 3 * p0[0]
        + 3 * (1 - t) ** 2 * t * p1[0]
        + 3 * (1 - t) * t**2 * p2[0]
        + t**3 * p3[0]
    )
    y = (
        (1 - t) ** 3 * p0[1]
        + 3 * (1 - t) ** 2 * t * p1[1]
        + 3 * (1 - t) * t**2 * p2[1]
        + t**3 * p3[1]
    )
    return x, y


def draw_icon(size: int = 512, pad_ratio: float = 0.18) -> Image.Image:
    img = Image.new("RGBA", (size, size), BG)

    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    m = int(size * 0.22)
    gd.ellipse([m, m, size - m, size - m], fill=(26, 27, 58, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(size * 0.08)))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    content = size * (1 - 2 * pad_ratio)
    ox = (size - content) / 2
    oy = (size - content) / 2

    def T(x: float, y: float):
        return (ox + x * content / 108.0, oy + y * content / 108.0)

    shield = [T(54, 30), T(72, 38)]
    for i in range(0, 33):
        t = i / 32.0
        shield.append(T(*cubic((72, 58), (72, 70), (64, 78), (54, 82), t)))
    for i in range(1, 33):
        t = i / 32.0
        shield.append(T(*cubic((54, 82), (44, 78), (36, 70), (36, 58), t)))
    shield.append(T(36, 38))

    stroke = max(4, int(size * 0.03))
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.line(shield + [shield[0]], fill=255, width=stroke, joint="curve")
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(overlay).bitmap((0, 0), mask, fill=PRIMARY)
    img = Image.alpha_composite(img, overlay)
    draw = ImageDraw.Draw(img)

    cx, cy = T(54, 54)
    r = abs(T(54, 42)[1] - cy)
    ring_w = max(3, int(size * 0.024))
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=PRIMARY_V, width=ring_w)

    dr = abs(T(58, 54)[0] - cx)
    draw.ellipse([cx - dr, cy - dr, cx + dr, cy + dr], fill=PRIMARY)
    return img.convert("RGB")


def load_font(size: int, bold: bool = False):
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def draw_feature() -> Image.Image:
    w, h = 1024, 500
    img = Image.new("RGBA", (w, h), BG)

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-80, -120, 520, 620], fill=(26, 27, 58, 170))
    gd.ellipse([620, -40, 1180, 560], fill=(99, 102, 241, 45))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=60))
    img = Image.alpha_composite(img, glow)

    bar = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(bar).rectangle([0, 0, w, 4], fill=PRIMARY)
    img = Image.alpha_composite(img, bar)

    mark = draw_icon(size=360, pad_ratio=0.20).convert("RGBA")
    mask = Image.new("L", (360, 360), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, 359, 359], radius=72, fill=255)
    rounded = Image.new("RGBA", (360, 360), BG)
    rounded.paste(mark, (0, 0))
    rounded.putalpha(mask)
    img.paste(rounded, (72, (h - 360) // 2), rounded)

    draw = ImageDraw.Draw(img)
    text_x = 470
    draw.text((text_x, 145), "Skry", font=load_font(92, bold=True), fill=WHITE)
    draw.text(
        (text_x, 262),
        "On-device gallery privacy",
        font=load_font(32),
        fill=PRIMARY_V,
    )
    draw.text(
        (text_x, 318),
        "Scan · Clean · Vault",
        font=load_font(28),
        fill=MUTED,
    )
    draw.text(
        (text_x, 358),
        "Nothing leaves this phone",
        font=load_font(26),
        fill=MUTED,
    )
    return img.convert("RGB")


def main() -> None:
    icon_path = os.path.join(OUT, "app-icon-512.png")
    feat_path = os.path.join(OUT, "feature-graphic-1024x500.png")
    draw_icon(512).save(icon_path, "PNG", optimize=True)
    draw_feature().save(feat_path, "PNG", optimize=True)
    print(icon_path)
    print(feat_path)


if __name__ == "__main__":
    main()
