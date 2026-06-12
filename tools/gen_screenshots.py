import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = r'C:\Users\vk241\AndroidStudioProjects\Spelltracker\docs\screenshots'
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 540, 1170

# Палитра
BG_DARK = (22, 8, 40, 255)
PURPLE  = (123, 47, 180, 255)
PURPLE_L = (170, 100, 220, 255)
GOLD    = (244, 196, 48, 255)
CREAM   = (244, 228, 193, 255)
WHITE   = (255, 255, 255, 255)
GREY    = (180, 170, 200, 255)
GREY_D  = (90, 80, 110, 255)

def try_font(size, bold=False):
    candidates = [
        r'C:\Windows\Fonts\segoeuib.ttf' if bold else r'C:\Windows\Fonts\segoeui.ttf',
        r'C:\Windows\Fonts\arial.ttf',
    ]
    for c in candidates:
        if os.path.exists(c):
            try: return ImageFont.truetype(c, size)
            except: pass
    return ImageFont.load_default()

FONT_TITLE      = try_font(34, bold=True)
FONT_SECTION    = try_font(22, bold=True)
FONT_BODY       = try_font(18)
FONT_SMALL      = try_font(14)
FONT_CELL       = try_font(26, bold=True)
FONT_CELL_SMALL = try_font(14)

def draw_bg(d):
    cx, cy = W/2, H*0.4
    max_r = max(W, H) * 0.8
    for i in range(50, 0, -1):
        t = i/50
        r = max_r * t
        rr = int(90*t + 22*(1-t))
        gg = int(42*t + 8*(1-t))
        bb = int(138*t + 40*(1-t))
        d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(rr, gg, bb, 255))

def draw_status_bar(d):
    d.rectangle([0, 0, W, 40], fill=(0, 0, 0, 180))
    d.text((20, 10), '12:30', fill=WHITE, font=FONT_SMALL)
    d.text((W-100, 10), '5G  100%', fill=WHITE, font=FONT_SMALL)

def draw_title(d, text, y=70):
    d.text((30, y), text, fill=WHITE, font=FONT_TITLE)

def draw_section_header(d, text, y):
    chip_w = len(text) * 9 + 30
    d.rounded_rectangle([20, y-4, 30+chip_w, y+28], radius=14, fill=PURPLE)
    d.text((35, y), text, fill=WHITE, font=FONT_SECTION)

def draw_class_card(d, x, y, w, h, name, level, multiclass=False):
    d.rounded_rectangle([x, y, x+w, y+h], radius=12, fill=(45, 20, 70, 220),
                        outline=PURPLE_L if multiclass else PURPLE, width=2)
    d.text((x+10, y+8), name, fill=GREY, font=FONT_SMALL)
    d.text((x+10, y+25), str(level), fill=GOLD if level > 0 else GREY_D, font=FONT_CELL)

# === MAIN SCREEN ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)
draw_status_bar(d)
draw_title(d, 'Spell Tracker', 70)
d.text((30, 115), 'PHB Multiclass Tracker', fill=GREY, font=FONT_SMALL)

d.rounded_rectangle([20, 155, W-20, 220], radius=12, fill=PURPLE)
d.text((40, 170), 'Эффект. уровень заклинателя', fill=WHITE, font=FONT_SMALL)
d.text((W-100, 165), '8', fill=GOLD, font=FONT_CELL)
d.text((W-130, 200), '(Bard 5 + Warlock pact)', fill=WHITE, font=FONT_SMALL)

draw_section_header(d, 'Классы', 250)
classes = [
    ('Bard', 5, True), ('Wizard', 0, False), ('Druid', 0, False),
    ('Cleric', 0, False), ('Warlock', 3, True), ('Paladin', 0, False),
    ('Ranger', 0, False), ('Sorcerer', 0, False), ('Artificer', 0, False),
]
cell_w = (W - 60) // 3
cell_h = 80
for i, (name, lvl, mc) in enumerate(classes):
    col = i % 3
    row = i // 3
    x = 20 + col * (cell_w + 10)
    y = 290 + row * (cell_h + 10)
    draw_class_card(d, x, y, cell_w, cell_h, name, lvl, mc)

draw_section_header(d, 'Pact Magic (Warlock)', 575)
d.rounded_rectangle([20, 610, W-20, 720], radius=12, fill=(45, 20, 70, 220),
                    outline=GOLD, width=2)
d.text((40, 625), 'Слотов:', fill=WHITE, font=FONT_BODY)
d.text((150, 622), '2', fill=GOLD, font=FONT_CELL)
d.text((300, 625), 'Уровень ячейки:', fill=WHITE, font=FONT_BODY)
d.text((W-50, 622), '2', fill=GOLD, font=FONT_CELL, anchor='ra')
for i in range(2):
    cx = 50 + i*60
    d.rounded_rectangle([cx, 665, cx+50, 705], radius=8, fill=GOLD, outline=CREAM, width=2)
    d.text((cx+15, 670), 'L2', fill=BG_DARK, font=FONT_CELL_SMALL)

draw_section_header(d, 'Ячейки заклинаний', 745)
slots_y = 790
levels = [(1, 4), (2, 3), (3, 3)]
for i, (lvl, cnt) in enumerate(levels):
    y = slots_y + i*70
    d.rounded_rectangle([20, y, W-20, y+55], radius=10, fill=(45, 20, 70, 200))
    d.text((35, y+13), f'Ур. {lvl}', fill=WHITE, font=FONT_BODY)
    for j in range(cnt):
        x = 130 + j*70
        d.rounded_rectangle([x, y+10, x+55, y+45], radius=8,
                            fill=GOLD if j > 0 else BG_DARK, outline=CREAM, width=2)
        if j == 0:
            d.text((x+22, y+14), f'{lvl}', fill=CREAM, font=FONT_CELL_SMALL)

btn_y = H - 130
d.rounded_rectangle([20, btn_y, W-20, btn_y+50], radius=12, fill=GOLD)
d.text((W//2, btn_y+13), 'Сбросить ячейки', fill=BG_DARK, font=FONT_BODY, anchor='mm')
d.rounded_rectangle([20, btn_y+60, W-20, btn_y+100], radius=12, outline=PURPLE_L, width=2)
d.text((W//2, btn_y+72), 'К заклинаниям', fill=PURPLE_L, font=FONT_BODY, anchor='mm')

img.convert('RGB').save(os.path.join(OUT_DIR, 'main.png'), 'PNG', optimize=True)
print('main.png written')

# === SPELLS LIST ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)
draw_status_bar(d)
draw_title(d, 'Заклинания', 70)
d.text((30, 115), 'Bard 5 / 42 заклинаний', fill=GREY, font=FONT_SMALL)

d.rounded_rectangle([20, 155, W-20, 200], radius=12, fill=(45, 20, 70, 200), outline=PURPLE, width=2)
d.text((35, 168), 'Поиск...', fill=GREY, font=FONT_BODY)

spells = [
    ('Огненный снаряд', 3, 'Огонь', True),
    ('Щит', 1, 'Защита', True),
    ('Невидимость', 2, 'Иллюзия', False),
    ('Лечащее слово', 1, 'Исцеление', True),
    ('Очарование личности', 1, 'Очарование', False),
    ('Починка', 0, 'Преобразование', True),
    ('Громовой удар', 0, 'Огонь', False),
]
list_y = 220
for name, lvl, school, prepared in spells:
    y = list_y
    d.rounded_rectangle([20, y, W-20, y+72], radius=10,
                        fill=(60, 25, 90, 200) if prepared else (40, 18, 60, 180),
                        outline=GOLD if prepared else PURPLE, width=2)
    d.text((35, y+10), name, fill=WHITE, font=FONT_BODY)
    d.text((35, y+38), school, fill=GREY, font=FONT_SMALL)
    lvl_text = 'Заговор' if lvl == 0 else f'Ур. {lvl}'
    d.text((W-30, y+25), lvl_text, fill=GOLD, font=FONT_BODY, anchor='ra')
    if prepared:
        d.text((W-95, y+25), '*', fill=GOLD, font=FONT_CELL)
    list_y += 80

btn_y = H - 100
d.rounded_rectangle([20, btn_y, W-20, btn_y+55], radius=12, fill=GOLD)
d.text((W//2, btn_y+18), 'Назад', fill=BG_DARK, font=FONT_BODY, anchor='mm')

img.convert('RGB').save(os.path.join(OUT_DIR, 'spells.png'), 'PNG', optimize=True)
print('spells.png written')

# === DRAWER ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)
d.rectangle([W*0.7, 0, W, H], fill=(0, 0, 0, 160))
d.rectangle([0, 0, W*0.7, H], fill=(35, 15, 60, 245))
draw_status_bar(d)

draw_title(d, 'Фильтры', 70)
d.text((30, 115), 'Класс и параметры', fill=GREY, font=FONT_SMALL)

draw_section_header(d, 'Выбранный класс', 165)
d.rounded_rectangle([20, 195, W*0.7-20, 240], radius=10, fill=PURPLE)
d.text((40, 205), 'Bard (5 ур.)', fill=WHITE, font=FONT_BODY)

draw_section_header(d, 'Школа магии', 270)
schools = ['Все', 'Огонь', 'Защита', 'Очарование', 'Иллюзия', 'Исцеление', 'Преобразование']
for i, s in enumerate(schools):
    y = 305 + i*50
    active = s in ('Все', 'Огонь')
    fill = PURPLE if active else (45, 20, 70, 200)
    outline = GOLD if active else PURPLE
    d.rounded_rectangle([20, y, W*0.7-20, y+38], radius=8, fill=fill, outline=outline, width=2)
    d.text((35, y+9), s, fill=WHITE, font=FONT_BODY)
    if active:
        d.text((W*0.7-50, y+9), 'V', fill=GOLD, font=FONT_BODY)

draw_section_header(d, 'Уровень заклинания', 660)
levels = ['Все', 'Заговоры', '1', '2', '3', '4+']
for i, l in enumerate(levels):
    col = i % 2
    row = i // 2
    x = 20 + col * (W*0.35 - 10)
    y = 695 + row*45
    active = l in ('Все', '1')
    fill = PURPLE if active else (45, 20, 70, 200)
    d.rounded_rectangle([x, y, x+W*0.32, y+36], radius=8, fill=fill, outline=GOLD if active else PURPLE, width=2)
    d.text((x+10, y+7), l, fill=WHITE, font=FONT_BODY)

draw_section_header(d, 'Дополнительно', 830)
for i, (label, checked) in enumerate([('Только подготовленные', False), ('Только концентрация', True), ('Только ритуалы', False)]):
    y = 865 + i*45
    d.rounded_rectangle([20, y, 40, y+20], radius=4, fill=GOLD if checked else (45, 20, 70, 200), outline=PURPLE_L, width=2)
    if checked:
        d.text((23, y+1), 'V', fill=BG_DARK, font=FONT_SMALL)
    d.text((50, y), label, fill=WHITE, font=FONT_BODY)

img.convert('RGB').save(os.path.join(OUT_DIR, 'drawer.png'), 'PNG', optimize=True)
print('drawer.png written')
print('DONE')
