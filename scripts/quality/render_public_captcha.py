"""Enlarge a public CAPTCHA image for normal manual entry; requires Pillow.

Optional task-local dependency directory: .local/pylibs. Does not read any cache,
derive answers, or change application CAPTCHA settings.
"""
import argparse
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / '.local/pylibs'))
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('source', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
with Image.open(args.source) as source:
    count = min(4, source.n_frames)
    canvas = Image.new('RGB', (600, 200 * count), 'white')
    for index in range(count):
        source.seek(index)
        canvas.paste(source.convert('RGB').resize((600, 200)), (0, index * 200))
    canvas.save(args.output)
