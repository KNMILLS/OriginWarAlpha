"""Extract actual imagegen paintings; no artwork is drawn, warped or interpolated."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import hashlib, json, zipfile, io, xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'assets'
ENV = ASSETS / 'art/proof/environment'
PAINTED = ASSETS / 'art/proof/painted'
CELL = 256

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def text_sha(path):
    return hashlib.sha256(path.read_text(encoding='utf-8').replace('\r\n','\n').encode('utf-8')).hexdigest()

def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

def save_cutout(source, crop, name, width, target, height=None):
    im = Image.open(source).convert('RGBA').crop(crop)
    box = im.getchannel('A').point(lambda v: 255 if v > 12 else 0).getbbox()
    assert box, name
    im = im.crop(box)
    size = (round(width * CELL), round(im.height * width * CELL / im.width))
    if height is not None:
        size = (round(im.width * height * CELL / im.height), round(height * CELL))
    im = im.resize(size, Image.Resampling.LANCZOS)
    out = target / 'regions' / (name + '.png')
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    return {'name':name, 'path':str(out.relative_to(ASSETS)).replace('\\','/'),
            'widthCells':im.width/CELL, 'heightCells':im.height/CELL,
            'pivotX':0.5, 'pivotY':0, 'layer':'world', 'pixelWidth':im.width,
            'pixelHeight':im.height, 'source':source.name, 'sourceCrop':list(crop),
            'alphaTrim':list(box), 'sha256':sha(out)}

def contact(regions, out):
    w,h=420,390
    sheet=Image.new('RGB',(w*3,h*((len(regions)+2)//3)),(23,29,28))
    d=ImageDraw.Draw(sheet)
    for i,r in enumerate(regions):
        im=Image.open(ASSETS/r['path']).convert('RGBA')
        im.thumbnail((w-28,h-60))
        x=(i%3)*w+(w-im.width)//2;y=(i//3)*h+44
        sheet.paste(im,(x,y),im)
        d.text(((i%3)*w+12,(i//3)*h+12),r['name'],fill=(218,223,214))
    out.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(out,quality=94)

def catalog(target, regions):
    # A real per-region OpenRaster editing catalog; flattened painting remains flattened.
    width=max(Image.open(ASSETS/r['path']).width for r in regions)+32
    height=max(Image.open(ASSETS/r['path']).height for r in regions)+32
    tree=ET.Element('image',w=str(width),h=str(height),name='Actual painted export layers')
    stack=ET.SubElement(tree,'stack')
    path=target/'painted-catalog.ora'
    with zipfile.ZipFile(path,'w') as z:
        def put(name,data):
            zi=zipfile.ZipInfo(name,(2026,10,21,0,0,0));zi.compress_type=zipfile.ZIP_DEFLATED;z.writestr(zi,data)
        put('mimetype',b'image/openraster')
        for i,r in enumerate(regions):
            name=f'data/layer{i:03}.png'; put(name,(ASSETS/r['path']).read_bytes())
            ET.SubElement(stack,'layer',name=r['name'],src=name,x='16',y='16',opacity='1.0',visibility='visible' if i==0 else 'hidden',**{'composite-op':'svg:src-over'})
        put('stack.xml',ET.tostring(tree,encoding='utf-8',xml_declaration=True))
        merged=Image.new('RGBA',(width,height));first=Image.open(ASSETS/regions[0]['path']).convert('RGBA');merged.alpha_composite(first,(16,16))
        buffer=io.BytesIO();merged.save(buffer,format='PNG');put('mergedimage.png',buffer.getvalue())
        merged.thumbnail((256,256));buffer=io.BytesIO();merged.save(buffer,format='PNG');put('Thumbnails/thumbnail.png',buffer.getvalue())

def environment():
    source=ENV/'source/environment-master.png'
    specs=[('wall.bulkhead',(15,15,552,478),1.1),('door.closed',(568,15,1030,478),1.15),
           ('door.open',(1048,15,1520,478),1.15),('prop.workbench',(20,492,625,962),1.7),
           ('control.switch',(642,492,968,966),0.72),('hatch.closed',(990,505,1520,980),1.4)]
    regions=[save_cutout(source,crop,name,width,ENV) for name,crop,width in specs]
    # A second name aliases actual workstation painting; it is not a second artwork.
    alias=dict(next(r for r in regions if r['name']=='prop.workbench'))
    alias.update(name='terminal.console',aliasOf='prop.workbench')
    regions.append(alias)
    floor=Image.open(ENV/'source/floor-master.png').convert('RGBA')
    fw,fh=floor.size
    for i,(gx,gy) in enumerate([(1,1),(2,1),(1,2),(2,2)]):
        # Crop only a quiet panel interior; no generated seam is painted over.
        crop=(round((gx+.035)*fw/4),round((gy+.035)*fh/4),round((gx+.965)*fw/4),round((gy+.965)*fh/4))
        tile=floor.crop(crop).resize((CELL,CELL),Image.Resampling.LANCZOS)
        name='floor.security' if i==0 else f'floor.security.variant{i}'
        out=ENV/'regions'/f'{name}.png';tile.save(out)
        regions.append(dict(name=name,path=str(out.relative_to(ASSETS)).replace('\\','/'),widthCells=1,heightCells=1,pivotX=0,pivotY=0,layer='ground',source='floor-master.png',sourceCrop=list(crop),sha256=sha(out)))
    dump(ENV/'regions.json',dict(version=1,referenceCellPixels=CELL,pivotOrigin='bottom-left',pathRoot='assets',regions=regions))
    contact(regions,ENV/'review/environment-contact.jpg')
    catalog(ENV,regions)
    sources=[]
    for src,prompt in [('environment-master.png','environment-prompt.txt'),('floor-master.png','floor-prompt.txt')]:
        sources.append(dict(source=src,sha256=sha(ENV/'source'/src),prompt=prompt,promptSha256NormalizedLF=text_sha(ENV/'source'/prompt),tool='built-in image_gen',selected=True,limitations='Generated diffuse paintings; runtime lighting, native integration and owner acceptance remain open.'))
    dump(ENV/'source/GENERATION.json',dict(task='AP-PAINTED shared environment',sources=sources,medium='original generated raster painting',rights='No third-party reference images used; original imagegen output; preserve generation provenance.',editableCatalog='Region layers only; original brushstroke layers do not exist.'))
    print('environment',len(regions),'regions exported')

if __name__=='__main__':
    environment()
