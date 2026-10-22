"""Meaningful world-space anchor checks for trimmed asymmetrical source frames."""
import unittest
from PIL import Image
from pack import trim


class TrimTest(unittest.TestCase):
    def test_asymmetrical_bounds_preserve_world_corners_and_origin(self):
        image=Image.new("RGBA",(384,448))
        for x in range(130,247):
            for y in range(53,390):
                image.putpixel((x,y),(170,150,120,255))
        entry=dict(widthCells=1.5,heightCells=1.75,pivotX=.5,pivotY=.136765867)
        crop,m=trim(image,entry)
        left,top,right,bottom=m["trim"]
        for px,py in ((130,53),(246,389),(192,250)):
            original_x=(px/image.width-entry["pivotX"])*entry["widthCells"]
            original_y=((image.height-py)/image.height-entry["pivotY"])*entry["heightCells"]
            trimmed_x=((px-left)/crop.width-m["pivotX"])*m["widthCells"]
            trimmed_y=((bottom-py)/crop.height-m["pivotY"])*m["heightCells"]
            self.assertAlmostEqual(original_x,trimmed_x)
            self.assertAlmostEqual(original_y,trimmed_y)
        self.assertEqual(image.crop(m["trim"]).tobytes(),crop.tobytes())

    def test_origin_outside_visible_silhouette_stays_inside_trim(self):
        image=Image.new("RGBA",(100,100))
        image.putpixel((20,20),(100,100,100,255))
        crop,m=trim(image,dict(widthCells=1,heightCells=1,pivotX=.8,pivotY=.1))
        self.assertTrue(0<=m["pivotX"]<=1)
        self.assertTrue(0<=m["pivotY"]<=1)
        self.assertEqual(255,crop.getpixel((20-m["trim"][0],20-m["trim"][1]))[3])


if __name__=="__main__":
    unittest.main()
