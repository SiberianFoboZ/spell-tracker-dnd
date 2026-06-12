"""
Генерация 3 мокапов экранов Spell Tracker под НОВЫЙ дизайн (после редизайна).
Дизайн: фиолетовый радиальный градиент, золотые акценты, 3×3 сетка классов,
без тулбара, с большой золотой цифрой эффект. уровня.

Запуск: py -3.12 tools/gen_screenshots.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = r'C:\Users\vk241\AndroidStudioProjects\Spelltracker\docs\screenshots'
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 540, 1170

# Палитра (1-в-1 с res/values/colors.xml)
BG_DARK   = (22, 8, 40, 255)
PURPLE    = (123, 47, 180, 255)
PURPLE_L  = (170, 100, 220, 255)
GOLD      = (244, 196, 48, 255)
CREAM     = (244, 228, 193, 255)
WHITE     = (255, 255, 255, 255)
GREY      = (180, 170, 200, 255)
GREY_D    = (90, 80, 110, 255)
CARD_BG   = (45, 21, 70, 255)

def try_font(size, bold=False):
    for c in [r'C:\Windows\Fonts\segoeuib.ttf' if bold else r'C:\Windows\Fonts\segoeui.ttf',
              r'C:\Windows\Fonts\arial.ttf']:
        if os.path.exists(c):
            try: return ImageFont.truetype(c, size)
            except: pass
    return ImageFont.load_default()

FONT_TITLE      = try_font(30, bold=True)
FONT_SUBTITLE   = try_font(13)
FONT_PANEL_LBL  = try_font(13)
FONT_PANEL_SUB  = try_font(12)
FONT_BIG_GOLD   = try_font(34, bold=True)
FONT_SECTION    = try_font(14, bold=True)
FONT_CARD_NAME  = try_font(11)
FONT_CARD_VALUE = try_font(24, bold=True)

def draw_bg(d):
    # Радиальный градиент (1-в-1 с bg_screen_gradient.xml).
    cx, cy = W/2, H*0.35
    max_r = 700
    for i in range(50, 0, -1):
        t = i/50
        r = max_r * t
        rr = int(90*t + 22*(1-t))
        gg = int(42*t + 8*(1-t))
        bb = int(138*t + 40*(1-t))
        d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(rr, gg, bb, 255))

def draw_class_card(d, x, y, w, h, name, level, multiclass=False):
    outline = PURPLE_L if multiclass else PURPLE
    d.rounded_rectangle([x, y, x+w, y+h], radius=10, fill=CARD_BG, outline=outline, width=2)
    d.text((x+w/2, y+10), name, fill=GREY, font=FONT_CARD_NAME, anchor='mm')
    d.text((x+w/2, y+h-22), str(level), fill=GOLD if level > 0 else GREY_D, font=FONT_CARD_VALUE, anchor='mm')

# === MAIN SCREEN ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)

# Заголовок (без тулбара — это TextView)
d.text((30, 50), 'Spell Tracker', fill=WHITE, font=FONT_TITLE)
d.text((30, 90), 'PHB Multiclass Tracker', fill=GREY, font=FONT_SUBTITLE)

# Панель «Эффект. уровень заклинателя» (purple, gold big number справа)
d.rounded_rectangle([20, 130, W-20, 230], radius=12, fill=PURPLE)
d.text((40, 145), 'Эффект. уровень заклинателя', fill=WHITE, font=FONT_PANEL_LBL)
d.text((40, 170), 'Уровень заклинателя: 8', fill=WHITE, font=FONT_PANEL_SUB)
d.text((W-50, 180), '8', fill=GOLD, font=FONT_BIG_GOLD, anchor='rm')

# Чип секции «Классы»
d.rounded_rectangle([20, 250, 130, 285], radius=14, fill=PURPLE)
d.text((75, 267), 'Классы', fill=WHITE, font=FONT_SECTION, anchor='mm')

# 3×3 сетка классов
classes = [
    ('Bard',     5, True),  ('Wizard',   0, False), ('Druid',    0, False),
    ('Cleric',   0, True),  ('Warlock',  3, True),  ('Paladin',  0, False),
    ('Ranger',   0, False), ('Sorcerer', 0, False), ('Artificer',0, True),
]
card_w = (W - 40 - 24) // 3  # 16 dp margin × 2 + 4 dp × 4 gaps
card_h = 84
start_x, start_y = 20, 300
for i, (name, lvl, mc) in enumerate(classes):
    col, row = i % 3, i // 3
    x = start_x + col * (card_w + 12)
    y = start_y + row * (card_h + 12)
    draw_class_card(d, x, y, card_w, card_h, name, lvl, mc)

# Pact Magic panel
d.rounded_rectangle([20, 600, W-20, 730], radius=12, fill=CARD_BG, outline=GOLD, width=2)
d.text((40, 615), 'Магия договора', fill=WHITE, font=FONT_PANEL_LBL)
d.text((40, 638), '2 ячеек 2 ур.', fill=GREY, font=FONT_PANEL_SUB)
d.text((W-50, 660), '2', fill=GOLD, font=FONT_BIG_GOLD, anchor='rm')
# Кнопки действий (Исп / Восст)
d.rounded_rectangle([40, 685, 200, 720], radius=10, fill=BG_DARK, outline=GOLD, width=1)
d.text((120, 702), 'Исп.', fill=GOLD, font=FONT_PANEL_LBL, anchor='mm')
d.rounded_rectangle([220, 685, W-40, 720], radius=10, fill=BG_DARK, outline=GOLD, width=1)
d.text((W-160, 702), 'Восст.', fill=GOLD, font=FONT_PANEL_LBL, anchor='mm')

# Чип «Ячейки заклинаний»
d.rounded_rectangle([20, 755, 220, 790], radius=14, fill=PURPLE)
d.text((120, 772), 'Ячейки заклинаний', fill=WHITE, font=FONT_SECTION, anchor='mm')

# Строки заклинаний (две — для превью)
for i, lvl in enumerate([1, 2]):
    y = 810 + i*100
    d.rounded_rectangle([20, y, W-20, y+85], radius=10, fill=CARD_BG, outline=PURPLE, width=2)
    d.text((40, y+12), f'Ур. {lvl}', fill=WHITE, font=FONT_PANEL_LBL)
    d.text((40, y+35), 'использовано / всего ячеек', fill=GREY, font=FONT_PANEL_SUB)
    d.text((W-50, y+25), '0 / 4' if lvl == 1 else '0 / 3', fill=GOLD, font=FONT_BIG_GOLD, anchor='rm')
    d.rounded_rectangle([40, y+58, 230, y+78], radius=8, fill=BG_DARK, outline=GOLD, width=1)
    d.text((135, y+68), 'Исп.', fill=GOLD, font=FONT_PANEL_LBL, anchor='mm')
    d.rounded_rectangle([250, y+58, W-40, y+78], radius=8, fill=BG_DARK, outline=GOLD, width=1)
    d.text((W-165, y+68), 'Восст.', fill=GOLD, font=FONT_PANEL_LBL, anchor='mm')

# Нижние кнопки
btn_y = H - 100
d.rounded_rectangle([20, btn_y, W/2-3, btn_y+50], radius=12, fill=GOLD)
d.text((W/4, btn_y+25), 'Сбросить ячейки', fill=BG_DARK, font=FONT_PANEL_LBL, anchor='mm')
d.rounded_rectangle([W/2+3, btn_y, W-20, btn_y+50], radius=12, outline=PURPLE_L, width=2)
d.text((W*3/4, btn_y+25), 'К заклинаниям', fill=PURPLE_L, font=FONT_PANEL_LBL, anchor='mm')

img.convert('RGB').save(os.path.join(OUT_DIR, 'main.png'), 'PNG', optimize=True)
print('main.png written')

# === SPELLS LIST ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)
d.text((30, 50), 'Заклинания', fill=WHITE, font=FONT_TITLE)
d.text((30, 90), 'Bard 5 / 42 заклинаний', fill=GREY, font=FONT_SUBTITLE)

d.rounded_rectangle([20, 130, W-20, 175], radius=12, fill=CARD_BG, outline=PURPLE, width=2)
d.text((35, 152), 'Поиск...', fill=GREY, font=FONT_PANEL_LBL, anchor='mm')

spells = [
    ('Огненный снаряд',     3, 'Огонь',         True),
    ('Щит',                 1, 'Защита',        True),
    ('Невидимость',         2, 'Иллюзия',       False),
    ('Лечащее слово',       1, 'Исцеление',     True),
    ('Очарование личности', 1, 'Очарование',    False),
    ('Починка',             0, 'Преобразование',True),
    ('Громовой удар',       0, 'Огонь',         False),
]
y = 195
for name, lvl, school, prepared in spells:
    fill = (60, 25, 90, 255) if prepared else (40, 18, 60, 220)
    outline = GOLD if prepared else PURPLE
    d.rounded_rectangle([20, y, W-20, y+72], radius=10, fill=fill, outline=outline, width=2)
    d.text((35, y+10), name, fill=WHITE, font=FONT_PANEL_LBL)
    d.text((35, y+38), school, fill=GREY, font=FONT_PANEL_SUB)
    lvl_text = 'Заговор' if lvl == 0 else f'Ур. {lvl}'
    d.text((W-30, y+25), lvl_text, fill=GOLD, font=FONT_PANEL_LBL, anchor='ra')
    if prepared:
        d.text((W-90, y+18), '*', fill=GOLD, font=FONT_BIG_GOLD)
    y += 80

d.rounded_rectangle([20, H-90, W-20, H-45], radius=12, fill=GOLD)
d.text((W/2, H-67), 'Назад', fill=BG_DARK, font=FONT_PANEL_LBL, anchor='mm')

img.convert('RGB').save(os.path.join(OUT_DIR, 'spells.png'), 'PNG', optimize=True)
print('spells.png written')

# === DRAWER ===
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img, 'RGBA')
draw_bg(d)
d.rectangle([W*0.7, 0, W, H], fill=(0, 0, 0, 160))
d.rectangle([0, 0, W*0.7, H], fill=(35, 15, 60, 245))

d.text((30, 50), 'Фильтры', fill=WHITE, font=FONT_TITLE)
d.text((30, 90), 'Класс и параметры', fill=GREY, font=FONT_SUBTITLE)

# Чип + класс
d.rounded_rectangle([20, 140, 220, 175], radius=14, fill=PURPLE)
d.text((120, 158), 'Выбранный класс', fill=WHITE, font=FONT_SECTION, anchor='mm')
d.rounded_rectangle([20, 185, W*0.7-20, 225], radius=10, fill=PURPLE)
d.text((40, 205), 'Bard (5 ур.)', fill=WHITE, font=FONT_PANEL_LBL, anchor='lm')

# Школы
d.rounded_rectangle([20, 245, 200, 280], radius=14, fill=PURPLE)
d.text((110, 263), 'Школа магии', fill=WHITE, font=FONT_SECTION, anchor='mm')
schools = ['Все', 'Огонь', 'Защита', 'Очарование', 'Иллюзия', 'Исцеление', 'Преобразование']
for i, s in enumerate(schools):
    y = 295 + i*50
    active = s in ('Все', 'Огонь')
    d.rounded_rectangle([20, y, W*0.7-20, y+38], radius=8,
                        fill=PURPLE if active else CARD_BG,
                        outline=GOLD if active else PURPLE, width=2)
    d.text((35, y+19), s, fill=WHITE, font=FONT_PANEL_LBL, anchor='lm')
    if active:
        d.text((W*0.7-50, y+19), 'V', fill=GOLD, font=FONT_PANEL_LBL, anchor='mm')

# Уровень
d.rounded_rectangle([20, 650, 250, 685], radius=14, fill=PURPLE)
d.text((135, 668), 'Уровень заклинания', fill=WHITE, font=FONT_SECTION, anchor='mm')
for i, l in enumerate(['Все', 'Заговоры', '1', '2', '3', '4+']):
    col, row = i % 2, i // 2
    x = 20 + col * (W*0.35 - 10)
    y = 700 + row*45
    active = l in ('Все', '1')
    d.rounded_rectangle([x, y, x+W*0.32, y+36], radius=8,
                        fill=PURPLE if active else CARD_BG,
                        outline=GOLD if active else PURPLE, width=2)
    d.text((x+W*0.16, y+18), l, fill=WHITE, font=FONT_PANEL_LBL, anchor='mm')

# Чекбоксы
d.rounded_rectangle([20, 845, 220, 880], radius=14, fill=PURPLE)
d.text((120, 863), 'Дополнительно', fill=WHITE, font=FONT_SECTION, anchor='mm')
for i, (label, checked) in enumerate([
    ('Только подготовленные', False),
    ('Только концентрация',   True),
    ('Только ритуалы',        False),
]):
    y = 900 + i*45
    d.rounded_rectangle([20, y, 40, y+20], radius=4,
                        fill=GOLD if checked else CARD_BG,
                        outline=PURPLE_L, width=2)
    if checked:
        d.text((30, y+10), 'V', fill=BG_DARK, font=FONT_PANEL_SUB, anchor='mm')
    d.text((50, y+10), label, fill=WHITE, font=FONT_PANEL_LBL, anchor='lm')

img.convert('RGB').save(os.path.join(OUT_DIR, 'drawer.png'), 'PNG', optimize=True)
print('drawer.png written')
print('DONE')
