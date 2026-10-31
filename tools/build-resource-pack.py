#!/usr/bin/env python3
"""Build AevumAgriculture's Java 26.2 resource pack with original pixel-art crop/food assets."""
from __future__ import annotations
import hashlib, json, math, random, shutil
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile
import yaml
from PIL import Image, ImageDraw, ImageFilter

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
PACK=ROOT/'resource-pack'
OUT=ROOT/'AevumAgriculture-ResourcePack.zip'
EMBEDDED=RES/'aevum-agriculture-pack.zip'
INK=(8,9,13,255); ONYX=(16,17,22,255); BRONZE=(128,96,45,255); GOLD=(201,139,36,255); LIGHT=(255,200,71,255); PARCHMENT=(243,235,221,255)
S=128

CROP_PALETTES={
 'grains':((83,135,65),(222,177,78)), 'legumes':((63,133,77),(111,174,91)), 'oilseeds':((61,124,68),(238,190,64)),
 'fibres':((53,129,76),(197,204,151)), 'vine_fruits':((69,131,67),(126,55,105)), 'tree_fruits':((49,116,63),(193,74,49)),
 'citrus':((57,132,62),(231,174,52)), 'nuts':((65,126,73),(131,91,51)), 'vegetables':((55,133,67),(190,78,61)),
 'root_vegetables':((61,133,69),(218,133,55)), 'herbs':((50,142,80),(105,168,102)), 'spices':((67,129,67),(188,85,54)),
 'sugar_crops':((63,142,71),(180,205,100)), 'berries':((59,127,71),(161,53,96))
}


def rgba(c,a=255): return (c[0],c[1],c[2],a)
def stable_color(text):
    h=hashlib.sha256(text.encode()).digest()
    return tuple(80 + (h[i] % 120) for i in range(3))
def write_json(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def model_json(tex):
    return {'parent':'minecraft:item/generated','textures':{'layer0':f'aevumagriculture:item/{tex}'},'display':{
        'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},
        'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[0.75,0.75,0.75]},
        'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[0.75,0.75,0.75]},
        'firstperson_righthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[0.7,0.7,0.7]},
        'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[0.65,0.65,0.65]}}}
def itemdef(path): return {'model':{'type':'minecraft:model','model':f'aevumagriculture:item/{path}'}}
def register(path,img):
    base=PACK/'assets/aevumagriculture'
    write_json(base/'items'/f'{path}.json',itemdef(path))
    write_json(base/'models/item'/f'{path}.json',model_json(path))
    target=base/'textures/item'/f'{path}.png';target.parent.mkdir(parents=True,exist_ok=True)
    img.save(target,optimize=True)

def vignette(img):
    # Warm metallic rim and quiet enamel centre for small inventory items.
    d=ImageDraw.Draw(img)
    d.ellipse((10,10,118,118),fill=(25,23,20,255),outline=(75,55,30,255),width=5)
    d.ellipse((16,16,112,112),fill=(17,18,21,255),outline=LIGHT,width=3)
    d.ellipse((22,22,106,106),outline=(91,67,36,255),width=2)

def make_seed(crop):
    img=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(img)
    # Seed pouch: stitched parchment, dark leather neck, bronze seal and seeds.
    d.ellipse((19,92,109,118),fill=(0,0,0,95))
    d.polygon([(34,31),(94,31),(103,102),(25,102)],fill=(150,116,68,255),outline=(240,205,139,255))
    d.polygon([(34,31),(45,20),(83,20),(94,31)],fill=(83,58,35,255),outline=LIGHT)
    d.line((33,39,96,39),fill=(84,59,36,255),width=4)
    accent=stable_color(crop['id']);d.ellipse((48,49,80,81),fill=(17,18,20,255),outline=LIGHT,width=3)
    d.ellipse((56,57,72,73),fill=rgba(accent),outline=PARCHMENT,width=2)
    rng=random.Random(crop['id'])
    for _ in range(5):
        x=rng.randint(34,95);y=rng.randint(82,98);r=rng.randint(2,4)
        d.ellipse((x-r,y-r,x+r,y+r),fill=(211,184,132,255),outline=(79,59,37,255))
    d.line((28,104,100,104),fill=LIGHT,width=2)
    return img

def leaf(d,x,y,scale,color,highlight):
    w=5*scale;h=17*scale
    d.polygon([(x,y),(x-w,y-h*.45),(x,y-h),(x+w,y-h*.45)],fill=rgba(color),outline=rgba(highlight))
    d.line((x,y-1,x,y-h*.75),fill=rgba(highlight,220),width=max(1,int(scale)))

def make_crop_stage(crop,stage):
    img=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(img)
    style=crop.get('visual-style','leaf');base,fruit=CROP_PALETTES.get(crop.get('category'),((61,130,72),(190,100,60)))
    accent=stable_color(crop['id']);fruit=tuple((fruit[i]*2+accent[i])//3 for i in range(3))
    # Ground shadow and a small, natural-looking soil crown.
    d.ellipse((18,106,110,121),fill=(0,0,0,85))
    d.ellipse((22,98,106,116),fill=(82,53,31,255),outline=(148,105,58,255),width=2)
    if stage==0:
        d.ellipse((56,94,72,104),fill=(206,178,111,255),outline=LIGHT,width=2)
        d.arc((52,85,77,109),180,350,fill=rgba(base),width=3)
        return img
    # A few stages use their own growth silhouettes, not just a scaled final icon.
    if style=='tree':
        trunk=(101,69,39,255);top=105-(18+stage*11)
        d.rounded_rectangle((58,top,71,105),radius=3,fill=trunk,outline=(178,130,77,255))
        d.line((64,82,42,66),fill=trunk,width=6);d.line((66,78,87,59),fill=trunk,width=6)
        for x,y,r in [(43,64,20),(64,44,25),(86,62,20),(52,74,17),(78,75,17)]:
            if stage>=2:d.ellipse((x-r,y-r,x+r,y+r),fill=rgba(base),outline=rgba((151,191,105)))
        if stage>=4:
            for x,y in [(48,56),(71,39),(88,61),(57,77),(80,77)]:d.ellipse((x-5,y-5,x+5,y+5),fill=rgba(fruit),outline=LIGHT,width=1)
    elif style in ('grain','cane'):
        n=2+stage
        for i in range(n):
            x=38+i*(52/max(1,n-1));height=20+stage*10+(i%2)*5
            d.line((x,105,x-4+i*2,105-height),fill=rgba(base),width=4)
            if stage>=2:
                for k in range(4):leaf(d,x-3+i*2+k*3,88-height*.35,0.7,base,(158,190,98))
            if stage>=3:
                hx=x-4+i*2;hy=105-height
                d.line((hx,hy,hx+3,hy-15),fill=rgba((170,131,63)),width=3)
                for k in range(4):d.ellipse((hx-5+k*3,hy-15-k%2*3,hx-1+k*3,hy-8-k%2*3),fill=rgba(fruit),outline=LIGHT,width=1)
    elif style=='root':
        d.line((64,104,64,51-stage*4),fill=rgba(base),width=5)
        for x,y,sc in [(48,76,1.0),(79,70,.9),(44,57,.8),(86,54,.8),(64,49,1.0)]:
            if stage>=1:leaf(d,x,y,sc,base,(166,197,116))
        if stage>=3:
            d.polygon([(64,75),(52,91),(59,111),(69,113),(76,90)],fill=rgba(fruit),outline=LIGHT)
    elif style=='vine':
        d.line((24,96,104,34),fill=rgba((93,91,48)),width=6)
        for x,y in [(34,85),(49,73),(65,61),(82,49),(98,37)]:
            if stage>=1:leaf(d,x,y,1.0,base,(166,197,116))
        if stage>=4:
            for x,y in [(48,86),(71,68),(91,50)]:
                for dx,dy in [(0,0),(6,4),(-5,6),(3,10)]:d.ellipse((x+dx-4,y+dy-4,x+dx+4,y+dy+4),fill=rgba(fruit),outline=LIGHT,width=1)
    elif style in ('flower','herb'):
        for x in (42,63,84):
            d.line((x,106,x-3,55-stage*3),fill=rgba(base),width=4)
            if stage>=2:
                leaf(d,x-10,83,.85,base,(167,199,113));leaf(d,x+9,72,.75,base,(167,199,113))
            if stage>=3:
                for a in range(5):
                    ang=2*math.pi*a/5;cx=x+math.cos(ang)*7;cy=52-stage*3+math.sin(ang)*7
                    d.ellipse((cx-5,cy-5,cx+5,cy+5),fill=rgba(fruit),outline=LIGHT,width=1)
                d.ellipse((x-4,48-stage*3,x+4,56-stage*3),fill=LIGHT)
    elif style in ('pod','leaf','bulb','stem'):
        for x,y,sc in [(44,86,1.0),(63,71,1.15),(83,85,1.0),(54,59,.85),(76,55,.9)]:
            if stage>=1:leaf(d,x,y,sc,base,(163,194,111))
        if stage>=3 and style=='pod':
            for x,y in [(47,82),(72,73),(86,91)]:d.rounded_rectangle((x,y,x+9,y+19),radius=4,fill=rgba(fruit),outline=LIGHT,width=1)
        if stage>=3 and style=='bulb':
            for x in (48,64,80):d.ellipse((x-9,91,x+9,111),fill=rgba(fruit),outline=LIGHT,width=1)
    else:
        d.line((64,106,64,49-stage*4),fill=rgba(base),width=5)
        for x,y,sc in [(45,83,1),(83,80,.9),(49,62,.85),(77,59,.9)]:
            if stage>=1:leaf(d,x,y,sc,base,(162,194,112))
        if stage>=4:
            for x,y in [(54,70),(75,77),(66,53)]:d.ellipse((x-7,y-7,x+7,y+7),fill=rgba(fruit),outline=LIGHT,width=2)
    return img

def make_produce(crop):
    img=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(img)
    base,fruit=CROP_PALETTES.get(crop.get('category'),((61,130,72),(190,100,60)))
    accent=stable_color(crop['id']);fruit=tuple((fruit[i]+accent[i])//2 for i in range(3))
    style=crop.get('visual-style','fruit')
    d.ellipse((19,103,109,120),fill=(0,0,0,85))
    if style in ('grain','cane'):
        for i in range(6):
            x=31+i*12;d.line((x,104,x+13,38+i%2*8),fill=rgba((165,122,55)),width=3)
            for k in range(4):d.ellipse((x+7+k%2*4,39+i%2*8+k*5,x+13+k%2*4,47+i%2*8+k*5),fill=rgba(fruit),outline=LIGHT,width=1)
    elif style=='root':
        d.polygon([(63,22),(42,49),(46,100),(62,114),(78,99),(82,49)],fill=rgba(fruit),outline=LIGHT,width=2)
        for x in (48,64,80):d.line((x,26,x-5,8),fill=rgba(base),width=4)
    elif style in ('tree','vine','fruit'):
        if style=='tree':
            d.line((64,30,64,13),fill=rgba((75,114,57)),width=7)
            for x in (48,79):leaf(d,x,29,.9,base,(170,202,116))
        for x,y,r in [(42,59,22),(77,61,25),(60,87,24)]:
            d.ellipse((x-r,y-r,x+r,y+r),fill=rgba(fruit),outline=LIGHT,width=2)
            d.ellipse((x-r*.45,y-r*.6,x-r*.05,y-r*.18),fill=(255,230,183,150))
    elif style=='pod':
        for x,y in [(39,44),(55,57),(73,36),(87,61)]:
            d.rounded_rectangle((x,y,x+18,y+43),radius=8,fill=rgba(fruit),outline=LIGHT,width=2)
            for k in range(3):d.ellipse((x+4,y+8+k*10,x+9,y+14+k*10),fill=(220,205,141,255))
    elif style in ('herb','leaf','stem'):
        for i in range(7):
            x=25+i*13;y=85-(i%3)*12
            d.polygon([(x,y+24),(x-9,y+3),(x,y-13),(x+10,y+5)],fill=rgba(base),outline=(160,194,111,255))
            d.line((x,y+22,x,y-7),fill=LIGHT,width=1)
    else:
        d.ellipse((25,30,103,108),fill=rgba(fruit),outline=LIGHT,width=3)
        d.ellipse((39,41,58,60),fill=(255,235,190,130))
        d.line((64,34,70,20),fill=rgba(base),width=5)
    # Gold inventory-rim highlights, subtle enough not to look like rarity particles.
    d.arc((25,27,103,108),198,292,fill=(255,230,167,170),width=2)
    return img

def make_good(good):
    img=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(img)
    cat=good.get('category','food');name=good.get('name','Food')
    palette={'bread':(183,132,65),'flour':(217,199,153),'oil':(197,154,48),'juice':(164,63,56),'syrup':(151,91,40),'preserve':(134,85,55),'stew':(146,77,43),'soup':(180,112,54),'meal':(156,108,67),'salad':(64,137,72),'dessert':(189,116,134),'drink':(91,137,151),'snack':(177,139,75),'spices':(158,75,47),'herbs':(65,139,83),'sweetener':(188,145,63),'powder':(107,76,65),'milk':(215,206,172),'extract':(154,93,47),'paste':(157,65,44),'prepared':(181,130,66)}
    c=palette.get(cat,(173,125,66));d.ellipse((19,102,109,120),fill=(0,0,0,80))
    if cat in ('bread','dessert','snack'):
        d.rounded_rectangle((26,48,102,100),radius=18,fill=rgba(c),outline=LIGHT,width=3)
        d.arc((35,45,94,92),190,345,fill=(255,225,164,255),width=3)
        if cat=='dessert':
            for x in (47,65,83):d.ellipse((x-5,61,x+5,71),fill=(245,212,161,255))
    elif cat in ('stew','soup','meal','salad','prepared'):
        d.ellipse((22,56,106,107),fill=(91,70,51,255),outline=LIGHT,width=3)
        d.pieslice((27,52,101,102),0,180,fill=rgba(c),outline=(238,198,121,255),width=2)
        for x,y in ((46,73),(65,64),(81,80),(57,88)):d.ellipse((x-5,y-4,x+5,y+4),fill=rgba((73,129,68) if x%2 else (225,180,75)))
    elif cat in ('drink','juice','milk','extract'):
        d.polygon([(48,22),(80,22),(86,40),(91,94),(37,94),(42,40)],fill=rgba(c),outline=LIGHT)
        d.rectangle((47,13,81,25),fill=(115,83,47,255),outline=LIGHT,width=2)
        d.arc((45,40,83,85),190,280,fill=(255,235,190,170),width=3)
    elif cat in ('flour','powder','spices','herbs','sweetener','paste'):
        d.polygon([(34,33),(94,33),(103,100),(25,100)],fill=rgba(c),outline=LIGHT,width=3)
        d.polygon([(34,33),(46,22),(82,22),(94,33)],fill=(93,64,39,255),outline=LIGHT,width=2)
        d.ellipse((50,49,78,77),fill=(20,21,23,255),outline=LIGHT,width=2)
        d.ellipse((59,58,69,68),fill=(237,201,124,255))
    elif cat in ('oil','syrup'):
        d.polygon([(48,21),(79,21),(85,37),(95,100),(31,100),(41,37)],fill=rgba(c),outline=LIGHT,width=3)
        d.rectangle((49,13,78,25),fill=(95,63,35,255),outline=LIGHT,width=2)
        d.ellipse((46,56,83,87),fill=(24,25,25,255),outline=LIGHT,width=2)
        d.polygon([(64,61),(54,79),(74,79)],fill=LIGHT)
    else:
        d.rounded_rectangle((27,34,101,103),radius=15,fill=rgba(c),outline=LIGHT,width=3)
        d.arc((36,42,90,96),205,330,fill=(255,231,181,200),width=4)
    return img

def make_medallion(glyph,accent=(201,139,36)):
    img=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(img)
    d.ellipse((10,10,118,118),fill=(32,28,22,255),outline=(69,51,29,255),width=5)
    d.ellipse((17,17,111,111),fill=(17,18,22,255),outline=rgba(LIGHT),width=3)
    d.ellipse((23,23,105,105),outline=rgba(accent),width=2)
    cx=cy=64
    if glyph in ('seed','grain'): 
        for i in range(5):d.ellipse((35+i*12,54+(i%2)*9,45+i*12,76+(i%2)*9),fill=rgba(LIGHT),outline=rgba(accent))
    elif glyph in ('fruit','crops','food'): 
        d.ellipse((34,35,94,95),fill=rgba(accent),outline=rgba(LIGHT),width=3);d.line((63,37,70,24),fill=rgba((89,136,65)),width=5)
    elif glyph in ('ledger','book'): 
        d.rounded_rectangle((36,30,92,99),radius=5,outline=rgba(LIGHT),width=4);d.line((64,32,64,97),fill=rgba(accent),width=3)
        for y in (45,58,71,84):d.line((43,y,57,y),fill=rgba(LIGHT),width=2);d.line((71,y,85,y),fill=rgba(LIGHT),width=2)
    elif glyph in ('market','business'):
        d.polygon([(28,52),(64,27),(100,52)],fill=rgba(accent),outline=rgba(LIGHT));d.rectangle((34,53,94,91),fill=(99,67,36,255),outline=rgba(LIGHT),width=3)
        for x in (44,61,78):d.rectangle((x,62,x+9,91),fill=(22,23,24,255),outline=rgba(LIGHT))
    elif glyph in ('recipes','recipe'):
        d.ellipse((32,32,96,96),outline=rgba(LIGHT),width=5);d.arc((42,40,86,88),20,300,fill=rgba(accent),width=5);d.ellipse((58,57,70,69),fill=rgba(LIGHT))
    elif glyph in ('stations','gear'):
        d.ellipse((42,42,86,86),outline=rgba(LIGHT),width=7);d.ellipse((56,56,72,72),fill=rgba(accent))
        for a in range(0,360,45):
            x=64+math.cos(math.radians(a))*32;y=64+math.sin(math.radians(a))*32;d.ellipse((x-6,y-6,x+6,y+6),fill=rgba(accent),outline=rgba(LIGHT))
    elif glyph in ('profile','laurel'):
        d.ellipse((49,35,79,65),outline=rgba(LIGHT),width=4);d.arc((34,49,94,104),180,360,fill=rgba(accent),width=6)
    elif glyph in ('help','close','home','back','next','open','closed','stock','all','vegetable','herb','fruit','grain','harvest','level'):
        symbols={'help':'?','close':'×','home':'⌂','back':'‹','next':'›','open':'✓','closed':'×','stock':'≡','all':'✦','vegetable':'♧','herb':'♣','fruit':'●','grain':'Ⅱ','harvest':'✂','level':'★'}
        f=symbols.get(glyph,'✦');d.text((44,34),f,fill=rgba(LIGHT),stroke_width=1,stroke_fill=rgba(accent))
    else:
        d.polygon([(64,27),(76,54),(103,64),(76,74),(64,101),(52,74),(25,64),(52,54)],fill=rgba(LIGHT),outline=rgba(accent))
    return img

def make_background():
    img=Image.new('RGBA',(256,256),(0,0,0,0));px=img.load()
    for y in range(222):
        for x in range(176):
            edge=min(x,175-x,y,221-y);vignette=max(0,8-edge//7)
            grain=((x*19+y*31+(x*y)%23)%7)-3
            vein=int(2*math.sin(x*.10+y*.035)+1.5*math.sin(y*.16-x*.03))
            shade=20+grain+vein-vignette;px[x,y]=(max(6,shade),max(7,shade-1),max(9,shade-2),255)
    d=ImageDraw.Draw(img)
    d.rectangle((0,0,175,221),fill=(10,11,14,255),outline=(67,48,26,255),width=2)
    d.rectangle((2,2,173,219),outline=(190,132,47,255),width=1)
    d.rectangle((4,4,171,217),outline=(43,34,23,255),width=1)
    d.line((7,14,168,14),fill=(137,99,43,255),width=1)
    for x in (6,169):
        d.line((x,6,x+(8 if x==6 else -8),6),fill=rgba(LIGHT),width=1);d.line((x,6,x,13),fill=rgba(GOLD),width=1)
        d.ellipse((x-1,2,x+1,4),fill=rgba(LIGHT))
    for y in range(20,127,18):
        d.rectangle((2,y+4,3,y+11),fill=(138,94,37,230));d.rectangle((172,y+4,173,y+11),fill=(138,94,37,230))
    def well(x,y):
        d.rectangle((x,y,x+16,y+16),fill=(6,7,9,255),outline=(78,57,30,255))
        d.line((x+1,y+1,x+15,y+1),fill=(2,3,4,255));d.line((x+1,y+1,x+1,y+15),fill=(2,3,4,255))
        d.line((x+1,y+15,x+15,y+15),fill=(58,43,24,255));d.line((x+15,y+1,x+15,y+15),fill=(40,31,19,255))
        d.rectangle((x+2,y+2,x+14,y+14),outline=(16,17,20,255))
    for row in range(6):
        for col in range(9):well(7+18*col,17+18*row)
    d.line((7,136,168,136),fill=(111,79,36,255),width=1);d.line((7,138,168,138),fill=(38,31,22,255),width=1)
    d.polygon([(88,135),(91,137),(88,139),(85,137)],fill=rgba(LIGHT))
    for y in (140,158,176,198):
        for col in range(9):well(7+18*col,y)
    path=PACK/'assets/minecraft/textures/gui/container/generic_54.png';path.parent.mkdir(parents=True,exist_ok=True);img.save(path,optimize=True)

def build():
    if PACK.exists():shutil.rmtree(PACK)
    PACK.mkdir(parents=True,exist_ok=True)
    crops=(yaml.safe_load((RES/'crops.yml').read_text(encoding='utf-8')) or {}).get('crops',{})
    goods=(yaml.safe_load((RES/'foods.yml').read_text(encoding='utf-8')) or {}).get('items',{})
    write_json(PACK/'pack.mcmeta',{'pack':{'description':'AevumMC • Imperium Agrarian Ledger • AevumAgriculture','min_format':88,'max_format':88}})
    (PACK/'LICENSE.txt').write_text('Original AevumMC AevumAgriculture pixel-art crop, food, station and interface artwork. Created for AevumMC. Do not redistribute as an official release without permission.\n',encoding='utf-8')
    icon=Image.new('RGBA',(128,128),INK);d=ImageDraw.Draw(icon);d.ellipse((5,5,123,123),fill=(23,23,22,255),outline=LIGHT,width=4);d.ellipse((14,14,114,114),fill=(13,14,17,255),outline=BRONZE,width=4);d.polygon([(64,24),(43,48),(55,50),(64,38),(75,50),(86,48)],fill=LIGHT);d.line((64,48,64,100),fill=LIGHT,width=7);d.arc((32,58,96,112),200,340,fill=(102,154,82,255),width=8);icon.save(PACK/'pack.png')
    # A complete crop art family: one seed, one harvested produce icon, six distinct plant stages.
    for cid,crop in crops.items():
        crop['id']=cid
        register('seed/'+cid,make_seed(crop))
        register('produce/'+cid,make_produce(crop))
        for stage in range(6):register(f'crop/{cid}_stage_{stage}',make_crop_stage(crop,stage))
    for gid,good in goods.items():register(good.get('model','ingredient/'+gid),make_good(good))
    stations=['mill','press','oven','kitchen','drying_rack','fermenter','preparation','mortar']
    for sid in stations:register('station/'+sid,make_medallion('stations'))
    gui=['ledger','seed','crops','recipes','market','business','stations','profile','laurel','help','close','all','grain','fruit','vegetable','herb','back','home','next','harvest','level','recipe','food','closed','open','stock']
    for icon_id in gui:register('gui/'+icon_id,make_medallion(icon_id))
    make_background()
    with ZipFile(OUT,'w',ZIP_DEFLATED,compresslevel=9) as zf:
        for path in sorted(PACK.rglob('*')):
            if path.is_file():zf.write(path,path.relative_to(PACK).as_posix())
    EMBEDDED.write_bytes(OUT.read_bytes())
    with ZipFile(OUT) as zf:
        count=sum(1 for n in zf.namelist() if n.startswith('assets/aevumagriculture/items/') and n.endswith('.json'))
    print(f'Built pack: {OUT.stat().st_size:,} bytes; {len(crops)} crops, {len(goods)} food/ingredient items, 8 stations, {len(gui)} GUI medallions; {count} item models.')

if __name__=='__main__':build()
