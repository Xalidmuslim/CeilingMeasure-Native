"""Postprocess three CC0 reference truck photos for catalog-only demonstration.

The white studio images are illustrative, not photos of fleet-owned vehicles.
Removes prominent third-party advertising from sample g01, softens visible
registration plates, and downsizes compressed images for faster Android decode.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import sys

def polish_one(path:Path):
    im=Image.open(path).convert("RGB")
    if im.size!=(1100,700):
        raise ValueError(f"Unexpected studio source size {path.name}: {im.size}")

    if path.name=="truck_g01.jpg":
        w,h=im.size
        # The photographed cargo body carried a third-party food delivery advert.
        # Repaint ONLY its advertising panel and cab ink, retain real truck contours.
        mask=Image.new("L", (w,h), 0)
        d=ImageDraw.Draw(mask)
        d.polygon([(677,73),(949,76),(958,419),(677,422)],fill=255)
        mask=mask.filter(ImageFilter.GaussianBlur(3))
        matte=Image.new("RGB",(w,h),(243,243,242))
        px=matte.load()
        for y in range(65,430):
            for x in range(665,970):
                shade=int(-((x-677)/300)*8-((y-72)/352)*9)
                k=max(219,min(248,244+shade))
                px[x,y]=(k,k,k-1)
        im=Image.composite(matte,im,mask)
        pencil=ImageDraw.Draw(im)
        pencil.line([(678,75),(954,79),(958,419),(677,421)],
                    fill=(159,165,160),width=3)
        pencil.line([(828,78),(828,415)],fill=(213,217,210),width=2)

        # Replace hood advertising and a small logo on the passenger door.
        cover=Image.new("RGB",(w,h))
        pp=cover.load()
        for y in range(h):
            if y<385:
                c=max(225,min(250,246-int((y-338)*.12)))
                color=(c,c,c)
                for x in range(w):
                    pp[x,y]=color
            else:
                for x in range(w):
                    c=max(191,min(225,214-int((x-540)*.06)-int((y-388)*.18)))
                    pp[x,y]=(c,c,c+2)
        mask=Image.new("L",(w,h),0)
        d=ImageDraw.Draw(mask)
        d.polygon([(187,315),(432,319),(401,354),(181,349)],fill=255)
        d.polygon([(543,395),(617,396),(618,441),(540,444)],fill=255)
        im=Image.composite(cover,im,mask.filter(ImageFilter.GaussianBlur(4)))

    # Privacy / professionalism: defocus printed license plates in example photos.
    # This is a stylized demo asset and not a replacement for the real fleet images.
    plate_box={
        "truck_g01.jpg":(226,445,321,470),
        "truck_g02.jpg":(778,476,889,515),
        "truck_g03.jpg":None
    }.get(path.name)
    if plate_box:
        blur=im.filter(ImageFilter.GaussianBlur(10))
        mask=Image.new("L",im.size,0)
        ImageDraw.Draw(mask).rounded_rectangle(plate_box,radius=5,fill=255)
        im=Image.composite(blur,im,mask.filter(ImageFilter.GaussianBlur(3)))

    im=ImageEnhance.Contrast(im).enhance(1.025)
    # Embedded assets: ~550px wide is enough at presentation card sizes.
    # Half-sized decoded bitmaps avoid costly repeated allocations in Compose.
    im=im.resize((880,560),Image.Resampling.LANCZOS)
    im.save(path,quality=87,subsampling=1,optimize=True)
    print(f"Polished illustration {path.name}: {im.size}, {path.stat().st_size} bytes")

if __name__=="__main__":
    folder=Path(sys.argv[1])
    for i in range(1,4):
        photo=folder/f"truck_g{i:02d}.jpg"
        if photo.is_file():
            polish_one(photo)
        else:
            print(f"Missing {photo.name}; safe UI placeholder will be used")
