#!/usr/bin/env python3
"""Frame authentic screenshots. Requires Pillow; never edits in-game pixels."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import base64, html
ROOT = Path(__file__).resolve().parents[1]
GALLERY = ROOT / 'docs/gallery'
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def font(n, bold=False): return ImageFont.truetype(BOLD if bold else FONT, n)
cards = [
 ('01-overview.png', '02-bossbar-actionbar.png', 'Every cooldown. One clear view.', 'ActionBar + BossBar + sidebar', 'LIVE FEEDBACK'),
 ('02-menu.png', '07-gui-active-cooldown.png', 'Know when to strike.', 'Live timers. Per-item status. A menu built for PvP.', 'COOLDOWN MENU'),
 ('03-blocked.png', '03-blocked-overlay.png', 'Spam stops here.', 'Blocked attempts stay blocked. Feedback stays readable.', 'USE PROTECTION'),
 ('04-trident.png', '08-trident-throw.png', 'Throw first. Cool down after.', 'The trident timer starts on launch, not while charging.', 'TRIDENT TIMING'),
]
for name, source, title, subtitle, label in cards:
    im=Image.new('RGB',(1440,1040),'#100d19'); d=ImageDraw.Draw(im)
    # A dark, geometric background; the screenshot itself is pasted without resampling.
    for x in range(0,1440,40):
        d.line((x,0,x,1040),fill='#151020',width=1)
    d.rectangle((0,0,1439,7),fill='#8b5cf6')
    d.rectangle((0,8,239,13),fill='#f5c542')
    d.text((80,37),'ITEMCOOLDOWNS  /  2.0',font=font(19,True),fill='#c4a5ff')
    d.text((80,79),title,font=font(46,True),fill='#f8f5ff')
    d.text((82,146),subtitle,font=font(23),fill='#a9a2b7')
    shot=Image.open(GALLERY/'originals'/source).convert('RGB')
    assert shot.size==(1280,720)
    im.paste(shot,(80,210))
    d.rectangle((75,205,1364,934),outline='#8b5cf6',width=3)
    for x,y in [(70,200),(1350,200),(70,920),(1350,920)]:
        d.rectangle((x,y,x+19,y+19),fill='#f5c542')
    d.text((80,967),label,font=font(18,True),fill='#f5c542')
    d.text((790,967),'REAL GAMEPLAY  /  PAPER 1.21.11',font=font(18),fill='#a9a2b7')
    im.save(GALLERY/name,optimize=True)
imgs=[]
for name,source,title,subtitle,label in cards:
    uri='data:image/png;base64,'+base64.b64encode((GALLERY/name).read_bytes()).decode()
    imgs.append(f'<section><img src="{uri}" alt="{html.escape(title)}"><p>{html.escape(subtitle)}</p></section>')
page='''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>ItemCooldowns 2.0 — Gallery</title><style>
*{box-sizing:border-box}body{margin:0;background:#0c0913;color:#f5f1ff;font:16px system-ui,sans-serif}header,main,footer{max-width:1150px;margin:auto;padding:32px 24px}header{border-bottom:2px solid #8b5cf6}small{color:#f5c542;letter-spacing:2px}h1{font-size:clamp(30px,6vw,60px);margin:15px 0}header p,footer{color:#b3a9c8;line-height:1.7}section{margin:0 0 44px}img{display:block;width:100%;height:auto;border:1px solid #39264f}section p{color:#c9bddc}b{color:#f5c542}.tag{display:inline-block;padding:7px 12px;border:1px solid #513279;margin:4px;color:#d7c2ff;font-size:13px}
</style><header><small>SOOQWESS / PLUGIN REDESIGN</small><h1>ItemCooldowns <span style="color:#a67bff">2.0</span></h1><p>Настоящий Minecraft-клиент, отдельный тестовый сервер Paper 1.21.11. Скриншоты без ИИ и без подмены интерфейса: добавлена только внешняя рамка и подписи.</p><span class="tag">ActionBar</span><span class="tag">BossBar</span><span class="tag">GUI</span><span class="tag">Scoreboard</span><span class="tag">10 языков</span></header><main>'''+''.join(imgs)+'''</main><footer><b>Важно:</b> галерея подтверждает игровые сценарии на Paper 1.21.11, а не полную матрицу Minecraft 1.21–26.3 и всех ядер. Проверка PlaceholderAPI в игре и запуск на Folia ещё нужны. Для кадра блокировки у кристалла отключён vanilla-overlay: иначе клиент сам подавляет повторный клик и сервер не может показать сообщение.</footer></html>'''
(GALLERY/'index.html').write_text(page)
print('Created four framed screenshots and an offline gallery.')
