import os
from PIL import Image, ImageDraw

OUT_ROOT = r'C:\Users\vk241\AndroidStudioProjects\Spelltracker\app\src\main\res'

# Удаляем старые webp-файлы (Android-робот по умолчанию)
for d in ['mipmap-mdpi','mipmap-hdpi','mipmap-xhdpi','mipmap-xxhdpi','mipmap-xxxhdpi']:
    for n in ['ic_launcher.webp','ic_launcher_round.webp']:
        p = os.path.join(OUT_ROOT, d, n)
        if os.path.exists(p):
            os.remove(p)

DENSITIES = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}

def draw_icon(size, rounded=False):
    S = size * 10
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, 'RGBA')

    # Фон: радиальный градиент #5A2A8A -> #160828
    cx, cy = S/2, S/2
    max_r = S * 0.72
    for i in range(60, 0, -1):
        t = i / 60
        r = max_r * t
        rr = int(90 * t + 22 * (1-t))
        gg = int(42 * t + 8  * (1-t))
        bb = int(138* t + 40 * (1-t))
        d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(rr, gg, bb, 255))

    if rounded:
        mask = Image.new('L', (S, S), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, S, S], fill=255)
        bg = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        bg.paste(img, (0, 0), mask)
        img = bg

    def sparkle(cx, cy, R, alpha=255):
        pts = [
            (cx, cy - R), (cx + R*0.18, cy - R*0.18),
            (cx + R, cy), (cx + R*0.18, cy + R*0.18),
            (cx, cy + R), (cx - R*0.18, cy + R*0.18),
            (cx - R, cy), (cx - R*0.18, cy - R*0.18),
        ]
        d.polygon(pts, fill=(244, 196, 48, alpha))

    # Малые искры
    sparkle(S*0.22, S*0.34, S*0.06, alpha=140)
    sparkle(S*0.78, S*0.34, S*0.06, alpha=140)
    # Большая центральная звезда
    sparkle(S*0.5,  S*0.30, S*0.16, alpha=255)

    # Книга: две кремовые трапеции с тёмно-фиолетовым контуром
    book_color = (244, 228, 193, 255)
    line_color = (42, 10, 63, 255)
    stroke_w = max(2, int(S*0.014))

    d.polygon([(S*0.26, S*0.48), (S*0.50, S*0.445),
               (S*0.50, S*0.78), (S*0.26, S*0.74)],
              fill=book_color, outline=line_color, width=stroke_w)
    d.polygon([(S*0.50, S*0.445), (S*0.74, S*0.48),
               (S*0.74, S*0.74), (S*0.50, S*0.78)],
              fill=book_color, outline=line_color, width=stroke_w)

    # "Текст" на страницах
    line_w = max(1, int(S*0.009))
    for y in [0.555, 0.605, 0.655, 0.705]:
        d.line([(S*0.315, S*y), (S*0.45, S*(y-0.018))], fill=line_color, width=line_w)
        d.line([(S*0.55,  S*(y-0.018)), (S*0.685, S*y)], fill=line_color, width=line_w)

    return img.resize((size, size), Image.LANCZOS)

written = []
for folder, size in DENSITIES.items():
    out_dir = os.path.join(OUT_ROOT, folder)
    os.makedirs(out_dir, exist_ok=True)
    for rounded in (False, True):
        icon = draw_icon(size, rounded=rounded)
        name = 'ic_launcher_round.png' if rounded else 'ic_launcher.png'
        path = os.path.join(out_dir, name)
        icon.save(path, 'PNG', optimize=True)
        written.append(f'{folder}/{name} ({size}x{size})')

print('\n'.join(written))
print('TOTAL:', len(written))
