"""Generate the original 64x64 pixel texture; Python standard library only."""
from pathlib import Path
import struct
import zlib

pixels = []
for y in range(64):
    row = []
    for x in range(64):
        # Gentle icy shading; translucency is applied once by GhostModel.
        shade = (x // 4 + y // 4) % 3 * 3
        color = (210 + shade, 239 + shade, 246 + shade, 255)
        # Front of the head's UV island: two dark blue eyes and a brow.
        if (x in (9, 10, 13, 14) and y in (10, 11)):
            color = (34, 80, 102, 255)
        if 9 <= x <= 14 and y == 9:
            color = (157, 207, 221, 255)
        if x in (11, 12) and y == 14:
            color = (139, 188, 204, 255)
        row.extend(color)
    pixels.append(b'\0' + bytes(row))

def chunk(kind, data):
    return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))

png = b'\x89PNG\r\n\x1a\n'
png += chunk(b'IHDR', struct.pack('!2I5B', 64, 64, 8, 6, 0, 0, 0))
png += chunk(b'IDAT', zlib.compress(b''.join(pixels)))
png += chunk(b'IEND', b'')
target = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/depthward_ghost_villagers/textures/entity/ghost_villager.png'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
