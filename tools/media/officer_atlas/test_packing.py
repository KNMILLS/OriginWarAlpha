import unittest
from PIL import Image
from pack import PAGE, PAD, place_images


class PackingTest(unittest.TestCase):
    def test_padding_and_variable_rectangles_never_overlap(self):
        sizes=[(1000,1000)]*5+[(110+i*7,90+i*19) for i in range(20)]
        images={(size,str(n)):Image.new("RGBA",size) for n,size in enumerate(sizes)}
        order=sorted(images,key=lambda k:(-k[0][0]*k[0][1],k[1]))
        count,placements=place_images(images,order)
        self.assertEqual((count,placements),place_images(images,order))
        self.assertEqual(len(images),len(placements))
        for key,(page,x,y,w,h) in placements.items():
            self.assertEqual(images[key].size,(w,h))
            self.assertTrue(PAD<=x and PAD<=y and x+w+PAD<=PAGE and y+h+PAD<=PAGE)
            for other,(p,ox,oy,ow,oh) in placements.items():
                if key==other or p!=page:continue
                self.assertTrue(x+w+PAD<=ox-PAD or ox+ow+PAD<=x-PAD or y+h+PAD<=oy-PAD or oy+oh+PAD<=y-PAD)


if __name__=="__main__":unittest.main()
