"""Normalize bundled sample-truck photos onto matte white backgrounds.

These are illustrative catalog images, not photos of the actual rental fleet.
This script does not make claims about the licenses or origin of input photos.
For actual cars, replace input assets with owner-supplied images.
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFilter

OUTLINES={
    "truck_g01.jpg": [(281,61),(921,66),(928,424),(916,444),(852,456),(842,501),(824,548),(804,570),(782,588),(732,590),(705,575),(691,543),(580,545),(517,557),(501,584),(475,597),(426,597),(397,582),(389,541),(175,540),(173,570),(150,590),(116,588),(94,570),(78,537),(81,422),(85,379),(118,349),(142,302),(184,238),(207,210),(265,193),(279,181)],
    "truck_g02.jpg": [(54,115),(628,121),(639,144),(653,239),(685,249),(721,297),(759,354),(784,377),(805,414),(806,502),(785,546),(748,568),(714,561),(697,537),(553,536),(537,565),(502,581),(482,572),(465,537),(353,520),(301,515),(286,542),(258,565),(204,553),(185,543),(177,510),(117,491),(113,451),(55,412)],
    "truck_g03.jpg": [(843,168),(857,452),(850,480),(722,485),(709,494),(693,507),(652,505),(627,489),(253,483),(233,509),(157,508),(133,482),(85,479),(76,449),(49,443),(52,409),(88,348),(157,319),(257,226),(306,188),(352,179)],
}
ORIGINAL_SIZE={"truck_g01.jpg":(960,636),"truck_g02.jpg":(960,640),"truck_g03.jpg":(960,597)}

def make_white_photo(path:Path):
    image=Image.open(path).convert("RGBA")
    w,h=image.size
    ow,oh=ORIGINAL_SIZE[path.name]
    outline=[(round(x*w/ow),round(y*h/oh)) for x,y in OUTLINES[path.name]]
    matte=Image.new("L",image.size,0)
    ImageDraw.Draw(matte).polygon(outline,fill=255)
    image.putalpha(matte.filter(ImageFilter.GaussianBlur(1.0)))
    bbox=image.getbbox()
    if bbox: image=image.crop(bbox)
    image.thumbnail((985,560),Image.Resampling.LANCZOS)
    canvas=Image.new("RGBA",(1100,700),(255,255,255,255))
    shadow=Image.new("RGBA",canvas.size,(255,255,255,0))
    ImageDraw.Draw(shadow).ellipse((210,585,890,627),fill=(30,40,35,42))
    shadow=shadow.filter(ImageFilter.GaussianBlur(17))
    canvas=Image.alpha_composite(canvas,shadow)
    canvas.alpha_composite(image,((1100-image.width)//2,588-image.height))
    output=canvas.convert("RGB")
    output.save(path,quality=91,optimize=True)
    if Image.open(path).size!=(1100,700):
        raise AssertionError(f"Invalid normalized output: {path}")

if __name__=="__main__":
    folder=Path(sys.argv[1])
    for name in OUTLINES:
        path=folder/name
        if path.exists():
            try:
                make_white_photo(path)
                print("White-background catalog photo:",name,path.stat().st_size)
            except Exception as err:
                print("Could not normalize sample image",name,str(err))
        else:
            print("Missing sample image; UI fallback will render:",name)
