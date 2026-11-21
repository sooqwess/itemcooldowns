#!/usr/bin/env python3
"""Build an offline gallery of unedited real Minecraft client screenshots.
Uses Python's standard library only. Does not modify any screenshot pixels.
"""
from pathlib import Path
import base64, html
ROOT=Path(__file__).resolve().parents[1]
GALLERY=ROOT/'docs/gallery'
ITEMS=[
    ('02-bossbar-actionbar.png','Live cooldown HUD','ActionBar, BossBar and sidebar in a real Minecraft client.'),
    ('07-gui-active-cooldown.png','Player cooldown menu','Active cooldown status and remaining time.'),
    ('03-blocked-overlay.png','Blocked use feedback','A rejected repeat interaction with chat feedback and particles. Crystal native overlay disabled.'),
    ('08-trident-throw.png','Trident launch','The five-second timer begins after the projectile is launched.'),
]
sections=[]
for filename,title,description in ITEMS:
    data=(GALLERY/'originals'/filename).read_bytes()
    uri='data:image/png;base64,'+base64.b64encode(data).decode()
    sections.append(f'<section><h2>{html.escape(title)}</h2><img src="{uri}" alt="{html.escape(title)}"><p>{html.escape(description)}</p></section>')
page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>ItemCooldowns — gameplay gallery</title><style>body{margin:0;background:#0b0911;color:#efeaf7;font:16px system-ui,sans-serif}main{max-width:1280px;margin:auto;padding:32px 20px}h1{font-size:36px}h2{font-size:21px;font-weight:500}p{color:#afa2c0;line-height:1.6}img{display:block;width:100%;height:auto}section{margin:44px 0}small{color:#d9b66e}</style><main><small>ITEMCOOLDOWNS / REAL GAMEPLAY</small><h1>See the plugin in Minecraft</h1><p>Unedited 1280×720 client captures from an isolated Paper 1.21.11 server. No generated gameplay, added captions or presentation frames inside the screenshots. Broader runtime compatibility is not certified by these captures.</p>'''+''.join(sections)+'''</main></html>'''
(GALLERY/'index.html').write_text(page)
print('Built offline gallery from four unchanged original screenshots.')
