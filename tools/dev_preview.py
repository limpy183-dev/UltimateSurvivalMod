import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
import usm_gen.painters_tools  # noqa
import usm_gen.painters_gui as G

sheet = Image.new("RGBA", (760, 560), (40, 40, 48, 255))
sheet.paste(G.codex_panel(), (5, 5))
sheet.paste(G.codex_icons().resize((128 * 3, 48), Image.NEAREST), (5, 235))
x = 5
for i, g in enumerate(G.EMBLEMS):
    em = G.emblem(g).image().resize((96, 96), Image.NEAREST)
    sheet.paste(em, (5 + (i % 4) * 100, 290 + (i // 4) * 100), em)
sheet.save(os.path.join(os.environ["TEMP"], "preview.png"))
print("ok")
