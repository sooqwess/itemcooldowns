#!/usr/bin/env python3
"""Non-generative final polish of the two USER-SELECTED AI artworks.
Requires Pillow only. Original composition, lettering and geometry remain intact.
This is a finishing/export script, not source code capable of regenerating the art.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance, ImageCms, ImageChops
import base64, json, hashlib
ROOT=Path(__file__).resolve().parent
SRGB=ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()

def load(name):
    return Image.open(ROOT/'originals'/name).convert('RGB')

def artwork_mask(im,kind):
    w,h=im.size
    # Soft local mask contains the hourglass, not the ivory frame or the title.
    bounds=(.26,.18,.74,.84) if kind=='icon' else (.75,.12,.90,.88)
    mask=Image.new('L',im.size)
    box=tuple(round(v*(w if i%2==0 else h)) for i,v in enumerate(bounds))
    ImageDraw.Draw(mask).ellipse(box,fill=255)
    mask=mask.filter(ImageFilter.GaussianBlur(h*.035))
    # Dark pixels and outer glow should not become washed out by exposure changes.
    highlights=im.convert('L').point(lambda v: min(255,max(0,round((v-20)*1.8))))
    return ImageChops.multiply(mask,highlights)

def polish(im,kind):
    # Restraint is intentional: no repaint, no bloom added, no geometry edits.
    improved=ImageEnhance.Color(im).enhance(1.035)
    improved=ImageEnhance.Contrast(improved).enhance(1.025)
    improved=ImageEnhance.Brightness(improved).enhance(1.018)
    refined=Image.composite(improved,im,artwork_mask(im,kind))
    return refined.filter(ImageFilter.UnsharpMask(radius=.8,percent=24,threshold=4))

def resize(im,size,kind):
    out=im.resize(size,Image.Resampling.LANCZOS)
    if kind=='icon':
        small=size[0]<=128
        out=out.filter(ImageFilter.UnsharpMask(radius=.48 if small else .65,
                                              percent=85 if small else 48,threshold=3))
    else:
        out=out.filter(ImageFilter.UnsharpMask(radius=.55,percent=32,threshold=3))
    return out

def png(im,path):
    im.save(path,optimize=True,icc_profile=SRGB)

def main():
    icon_original=load('icon-selected.png')
    banner_original=load('banner-selected.png')
    icon=polish(icon_original,'icon');banner=polish(banner_original,'banner')
    assert icon.size==icon_original.size and banner.size==banner_original.size
    for side in (64,96,128,256,512,1024):
        png(resize(icon,(side,side),'icon'),ROOT/f'itemcooldowns-icon-{side}.png')
    for width in (1568,2048,4096):
        png(resize(banner,(width,width//4),'banner'),ROOT/f'itemcooldowns-banner-{width}x{width//4}.png')
    resize(banner,(2048,512),'banner').save(ROOT/'itemcooldowns-banner-2048x512.webp',lossless=True,method=6,icc_profile=SRGB)
    # Same-size exports used for a fair before/after preview; no source files changed.
    original_icon=icon_original.resize((512,512),Image.Resampling.LANCZOS)
    original_banner=banner_original.resize((2048,512),Image.Resampling.LANCZOS)
    def img_uri(im):
        from io import BytesIO
        bio=BytesIO();im.save(bio,format='PNG');return 'data:image/png;base64,'+base64.b64encode(bio.getvalue()).decode()
    def file_uri(name):return 'data:image/png;base64,'+base64.b64encode((ROOT/name).read_bytes()).decode()
    assets={'iconBefore':img_uri(original_icon),'bannerBefore':img_uri(original_banner),
            'iconAfter':file_uri('itemcooldowns-icon-512.png'),
            'bannerAfter':file_uri('itemcooldowns-banner-2048x512.png')}
    sizes=''.join(f'<figure><img width="{n}" height="{n}" src="{file_uri(f"itemcooldowns-icon-{n}.png")}" alt="Icon {n}px"><figcaption>{n} px</figcaption></figure>' for n in (64,96,128,256))
    html='''<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>ItemCooldowns — final polish</title><style>*{box-sizing:border-box}body{background:#090711;color:#f2ebfb;font:16px system-ui,sans-serif;margin:0}main{max-width:1200px;margin:auto;padding:42px 24px}small{color:#d9b368;letter-spacing:.16em;font-size:11px}h1{font-size:40px;font-weight:500;margin:14px 0}p{color:#ad9ebd;line-height:1.65;max-width:760px}button{background:#1b112b;color:#dfd3ef;border:1px solid #5a397d;padding:11px 17px;border-radius:6px;cursor:pointer;margin:15px 8px 24px 0}button.active{background:#7250a1;border-color:#b99be9;color:white}.banner{width:100%;display:block;border:1px solid #291e36}.panel{display:flex;align-items:center;gap:40px;flex-wrap:wrap;margin:32px 0}.mainicon{width:256px;height:256px}.panel p{max-width:490px}h2{font-size:19px;font-weight:500;margin-top:44px}.sizes{display:flex;align-items:center;gap:36px;flex-wrap:wrap;padding:24px 0}figure{margin:0;text-align:center}figcaption{margin-top:10px;font:12px monospace;color:#9988ab}.note{border-top:1px solid #2c203a;margin-top:28px;padding-top:20px;font-size:13px}</style><main><small>SOOQWESS / ITEMCOOLDOWNS</small><h1>Финальная доводка</h1><p>Выбранные иллюстрации сохранены. Локально усилены свет и цвет эмблемы, добавлена умеренная резкость при уменьшении. Композиция и надпись не менялись.</p><div><button id="after" class="active" onclick="setView(true)">После доводки</button><button id="before" onclick="setView(false)">Выбранный оригинал</button></div><img id="banner" class="banner" src="'''+assets['bannerAfter']+'''" alt="ItemCooldowns banner"><div class="panel"><img id="icon" class="mainicon" src="'''+assets['iconAfter']+'''" alt="ItemCooldowns icon"><p id="label">Деликатная обработка без перерисовки. Кнопки выше переключают обе картинки, чтобы можно было проверить изменения.</p></div><h2>Иконка в реальных размерах — после доводки</h2><div class="sizes">'''+sizes+'''</div><p class="note">PNG с цветовым профилем sRGB · иконки 64–1024 px · баннеры 1568 / 2048 / 4096 px · WebP.<br>Оригиналы и скрипт обработки включены в ZIP. Новые файлы не опубликованы на Modrinth или GitHub.</p></main><script>const art='''+json.dumps(assets)+''';function setView(after){document.getElementById('banner').src=art[after?'bannerAfter':'bannerBefore'];document.getElementById('icon').src=art[after?'iconAfter':'iconBefore'];document.getElementById('after').classList.toggle('active',after);document.getElementById('before').classList.toggle('active',!after);}</script></html>'''
    (ROOT/'preview.html').write_text(html)
    hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in ROOT.glob('*.png')}
    (ROOT/'sha256.json').write_text(json.dumps(hashes,indent=2))
    print('Exported 6 icon sizes, 3 banner sizes, lossless WebP and before/after preview.')

if __name__=='__main__':main()
