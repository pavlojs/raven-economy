#!/usr/bin/env python3
# Copyright 2026 pavlojs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Source of the block and item textures under assets/ravencoin/textures.

The PNGs this writes are committed; this is what they were written by. Sixteen
pixels square is small enough that a hand edit is unreviewable in a diff and
unrepeatable afterwards, so the textures are kept as code and regenerated:

    python3 tools/generate_textures.py

Needs Pillow. Every colour is drawn from one of the ramps below, which is what
keeps the three machines reading as the same machine in three trims.
"""
from PIL import Image
import pathlib

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/ravencoin/textures"

# --- palettes, sampled from the existing textures ---------------------------
GOLD = ["#5C4210", "#7E5E16", "#96701A", "#AC8220", "#C49B2A", "#D8B23A",
        "#E9C54B", "#F3D668", "#FCE78A"]
AMETHYST = ["#1E0A30", "#4A1F6E", "#682E96", "#843EBA", "#BE84E8", "#E0BEF8"]
EMERALD = ["#0E5230", "#1B7A48", "#26A65B", "#4FD98A", "#96F5C0"]
# The chassis alloys. Same value structure, different tint: the shop is plain
# steel, the operator's build is the cold one, so they read as one machine in
# two trims rather than two unrelated blocks.
STEEL = ["#141418", "#1C1C20", "#26262C", "#2C2C32", "#3A3A42", "#42424A",
         "#4A4A52", "#52525A", "#606068", "#787880", "#96969E", "#B8B8C0"]
COLD = ["#121020", "#1A1828", "#221F32", "#28253A", "#36334A", "#413E56",
        "#4A4762", "#54506E", "#635F80", "#7A7698", "#918DAE", "#B0ACC8"]
AMBER = ["#241804", "#5C3A08", "#8E5A0E", "#C98A1A", "#F0B03C", "#FFD27A"]
GLASS = ["#0A1014", "#16202A", "#2A3A46", "#5E7E92", "#A8C8D8"]


def rgb(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


CLEAR = (0, 0, 0, 0)


class Tex:
    def __init__(self, n=16):
        self.n = n
        self.p = [[CLEAR] * n for _ in range(n)]

    def px(self, x, y, c):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.p[y][x] = rgb(c) if isinstance(c, str) else c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.px(x, y0, c)
            self.px(x, y1, c)
        for y in range(y0, y1 + 1):
            self.px(x0, y, c)
            self.px(x1, y, c)

    def h(self, y, x0, x1, c):
        self.rect(x0, y, x1, y, c)

    def v(self, x, y0, y1, c):
        self.rect(x, y0, x, y1, c)

    def grain(self, x0, y0, x1, y1, palette, seed, spread=2):
        """Deterministic plank/stone grain: a hash, not a random generator, so the
        same call always produces the same texture."""
        mid = len(palette) // 2
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                hsh = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
                hsh = (hsh ^ (hsh >> 13)) * 1274126177 & 0xFFFFFFFF
                step = (hsh >> 16) % (2 * spread + 1) - spread
                self.px(x, y, palette[max(0, min(len(palette) - 1, mid + step))])

    def bevel(self, x0, y0, x1, y1, light, dark):
        """Lit from the top-left, the way every vanilla block face is."""
        for x in range(x0, x1 + 1):
            self.px(x, y0, light)
            self.px(x, y1, dark)
        for y in range(y0, y1 + 1):
            self.px(x0, y, light)
            self.px(x1, y, dark)
        self.px(x1, y0, light)
        self.px(x0, y1, dark)

    def save(self, path):
        img = Image.new("RGBA", (self.n, self.n))
        img.putdata([c for row in self.p for c in row])
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
        return img


def rivet(t, x, y, pal):
    """A 2x2 iron stud."""
    t.px(x, y, pal[4]); t.px(x + 1, y, pal[3])
    t.px(x, y + 1, pal[3]); t.px(x + 1, y + 1, pal[1])


# ---------------------------------------------------------------------------
# SHOP and SERVER SHOP — the same machine as the ATM, in two other trims.
# The pack is a tech pack and the server is a tech server, so a wooden market
# stall would have been the odd block out. What separates the three is the
# accent colour and what sits behind the front panel, not the chassis.
# ---------------------------------------------------------------------------

def machine_side(alloy, accent, seed):
    """Riveted panel with a louvred vent. Shows on east, south and west."""
    t = Tex()
    t.grain(0, 0, 15, 15, alloy[2:6], seed=seed)
    t.frame(0, 0, 15, 15, alloy[0])
    t.bevel(1, 1, 14, 14, alloy[6], alloy[0])
    t.rect(2, 2, 13, 13, alloy[3])
    t.bevel(2, 2, 13, 13, alloy[1], alloy[5])
    t.grain(3, 3, 12, 12, alloy[2:5], seed=seed + 1, spread=1)
    # louvres: a dark slot with the lip above it catching light
    for y in (4, 6, 8, 10):
        t.h(y, 4, 11, alloy[6])
        t.h(y + 1, 4, 11, alloy[0])
    t.v(3, 4, 11, alloy[1]); t.v(12, 4, 11, alloy[1])
    for cx, cy in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, cx, cy, alloy[3:])
    t.px(12, 2, accent[4]); t.px(13, 2, accent[1])
    return t


def machine_top(alloy, accent, seed):
    """Deck plate: bolted, vented, with the status lamp repeated so the block
    can be told apart from above."""
    t = Tex()
    t.grain(0, 0, 15, 15, alloy[3:7], seed=seed)
    t.frame(0, 0, 15, 15, alloy[0])
    t.bevel(1, 1, 14, 14, alloy[7], alloy[1])
    t.rect(3, 3, 12, 12, alloy[4])
    t.bevel(3, 3, 12, 12, alloy[2], alloy[6])
    t.grain(4, 4, 11, 11, alloy[3:6], seed=seed + 1, spread=1)
    for y in (5, 7, 9):
        t.h(y, 5, 10, alloy[7])
        t.h(y + 1, 5, 10, alloy[0])
    for cx, cy in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(t, cx, cy, alloy[3:])
    t.px(7, 12, accent[4]); t.px(8, 12, accent[3])
    return t


def machine_front(alloy, accent, seed, vitrine):
    """Chassis, readout, main panel, dispensing slot.

    `vitrine` decides what the main panel is: a glazed case with stock behind it
    for the player's shop, a plated server face for the operator's — which is
    exactly the difference between a block that holds stock and one that does
    not."""
    t = Tex()
    t.grain(0, 0, 15, 15, alloy[2:6], seed=seed)
    t.frame(0, 0, 15, 15, alloy[0])
    # side rails
    t.rect(0, 1, 1, 14, alloy[3]); t.rect(14, 1, 15, 14, alloy[3])
    t.v(1, 1, 14, alloy[6]); t.v(14, 1, 14, alloy[1])
    for y in (2, 7, 12):
        rivet(t, 0, y, alloy[3:]); rivet(t, 14, y, alloy[3:])

    # readout
    t.rect(2, 1, 13, 3, accent[0])
    t.bevel(2, 1, 13, 3, alloy[1], alloy[7])
    t.h(2, 4, 9, accent[4]); t.px(4, 2, accent[5])
    t.h(2, 11, 12, accent[3])
    t.h(3, 4, 7, accent[2])
    t.h(4, 0, 15, alloy[1])

    if vitrine:
        # glazed case: dark inside, stock on a shelf, one raking reflection
        t.rect(2, 5, 13, 11, alloy[4])
        t.bevel(2, 5, 13, 11, alloy[6], alloy[1])
        t.rect(3, 6, 12, 10, GLASS[0])
        t.h(10, 3, 12, alloy[5])
        # stock on the shelf: a coin, an emerald, a crated ingot
        t.rect(4, 7, 5, 9, GOLD[4]); t.px(4, 7, GOLD[6]); t.px(5, 9, GOLD[1])
        t.rect(7, 8, 8, 9, EMERALD[2]); t.px(7, 8, EMERALD[3]); t.px(8, 9, EMERALD[0])
        t.rect(10, 7, 11, 9, alloy[8]); t.px(10, 7, alloy[10]); t.px(11, 9, alloy[4])
        # one raking reflection, kept in the empty glass above the stock so it
        # never reads as more merchandise
        for i in range(4):
            t.px(4 + i, 7 - i, GLASS[1])
        t.px(4, 7, GOLD[6]); t.px(7, 6, GLASS[2]); t.px(8, 6, GLASS[2])
    else:
        # plated server face: three racked plates, which is the block's whole
        # identity spelled out in six pixels of height
        t.rect(2, 5, 13, 11, alloy[3])
        t.bevel(2, 5, 13, 11, alloy[6], alloy[1])
        t.grain(3, 6, 12, 10, alloy[2:5], seed=seed + 2, spread=1)
        for i, y in enumerate((6, 8, 10)):
            t.rect(4, y, 11, y, accent[2])
            t.h(y, 4, 4, accent[5])
            t.px(5, y, accent[4])
            t.h(y, 8, 11, accent[1])
        t.px(12, 6, accent[4]); t.px(12, 10, accent[1])
        for cx, cy in ((2, 5), (13, 5), (2, 11), (13, 11)):
            t.px(cx, cy, alloy[7])

    # dispensing slot
    t.h(12, 2, 13, alloy[7])
    t.rect(3, 13, 12, 14, alloy[0])
    t.h(13, 3, 12, alloy[1])
    t.px(3, 14, accent[3]); t.px(12, 14, accent[3])
    t.h(15, 0, 15, alloy[0])
    return t


def shop_side():
    return machine_side(STEEL, AMBER, seed=11)


def shop_top():
    return machine_top(STEEL, AMBER, seed=21)


def shop_front():
    return machine_front(STEEL, AMBER, seed=31, vitrine=True)


def server_shop_side():
    return machine_side(COLD, AMETHYST, seed=41)


def server_shop_top():
    return machine_top(COLD, AMETHYST, seed=51)


def server_shop_front():
    return machine_front(COLD, AMETHYST, seed=61, vitrine=False)


# ---------------------------------------------------------------------------
# COIN — struck round, not cut octagonal, with a geometric R.
# ---------------------------------------------------------------------------
DISC = {          # a true 14px circle: dy^2 + dx^2 <= 7.4^2 about (7.5, 7.5)
    0: None, 1: (4, 11), 2: (3, 12), 3: (2, 13), 4: (1, 14), 5: (1, 14),
    6: (1, 14), 7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (1, 14), 11: (1, 14),
    12: (2, 13), 13: (3, 12), 14: (4, 11), 15: None,
}

# A geometric R: straight stem, square bowl, straight diagonal leg. No serifs,
# no curve — that is what reads as modern at this size.
R_GLYPH = [
    "#####.",
    "#....#",
    "#....#",
    "#####.",
    "#..#..",
    "#...#.",
    "#....#",
]


def coin_disc(t, ox=0, oy=0, scale_pal=GOLD):
    for y, span in DISC.items():
        if span is None:
            continue
        x0, x1 = span
        for x in range(x0, x1 + 1):
            t.px(ox + x, oy + y, scale_pal[4])
    # rim: one pixel in from the silhouette, lit top-left
    for y, span in DISC.items():
        if span is None:
            continue
        x0, x1 = span
        for x in (x0, x1):
            lit = (y < 8) if x == x0 else (y < 4)
            t.px(ox + x, oy + y, scale_pal[6] if lit else scale_pal[1])
    for x in range(DISC[1][0], DISC[1][1] + 1):
        t.px(ox + x, oy + 1, scale_pal[7])
    for x in range(DISC[14][0], DISC[14][1] + 1):
        t.px(ox + x, oy + 14, scale_pal[1])
    for x in range(DISC[2][0] + 1, DISC[2][1]):
        t.px(ox + x, oy + 2, scale_pal[6])
    for x in range(DISC[13][0] + 1, DISC[13][1]):
        t.px(ox + x, oy + 13, scale_pal[2])
    # face, a shade below the rim so the R has something to sit on
    for y in range(3, 13):
        x0, x1 = DISC[y]
        t.rect(ox + x0 + 1, oy + y, ox + x1 - 1, oy + y, scale_pal[5])
    for y in range(4, 11):
        x0, x1 = DISC[y]
        t.rect(ox + x0 + 2, oy + y, ox + x1 - 2, oy + y, scale_pal[4])
    # a cold glint on the rim — the one thing the coin shares with the ATM
    t.px(ox + 10, oy + 2, "#5BE8DA"); t.px(ox + 11, oy + 3, "#A3F7F0")
    # the field is domed, not flat: one shade up along the top-left arc
    t.px(ox + 3, oy + 4, scale_pal[6]); t.px(ox + 2, oy + 5, scale_pal[6])
    t.px(ox + 2, oy + 6, scale_pal[6]); t.px(ox + 13, oy + 10, scale_pal[3])
    t.px(ox + 13, oy + 9, scale_pal[3]); t.px(ox + 12, oy + 11, scale_pal[3])


def coin():
    t = Tex()
    coin_disc(t)
    for gy, row in enumerate(R_GLYPH):
        for gx, ch in enumerate(row):
            if ch == "#":
                t.px(5 + gx, 4 + gy, GOLD[0])
    # struck into the metal, so the wall below each stroke catches the light
    for gy, row in enumerate(R_GLYPH):
        for gx, ch in enumerate(row):
            if ch != "#" or gy + 1 >= len(R_GLYPH) or R_GLYPH[gy + 1][gx] == "#":
                continue
            if 4 + gy + 1 <= 11:
                t.px(5 + gx, 4 + gy + 1, GOLD[6])
    return t


def coin_block():
    """Nine struck coins packed into a block. Round now, as the coin is.

    Drawn on a deliberately dark field: nine gold discs on gold read as noise,
    and a player has to be able to count them from across a room."""
    t = Tex()
    t.rect(0, 0, 15, 15, GOLD[0])
    t.grain(0, 0, 15, 15, [GOLD[0], GOLD[1], GOLD[0]], seed=71, spread=1)
    small = {0: (1, 2), 1: (0, 3), 2: (0, 3), 3: (1, 2)}
    for row in range(3):
        for col in range(3):
            ox, oy = 1 + col * 5, 1 + row * 5
            for y, (x0, x1) in small.items():
                for x in range(x0, x1 + 1):
                    t.px(ox + x, oy + y, GOLD[5])
            t.px(ox + 1, oy + 0, GOLD[8]); t.px(ox + 0, oy + 1, GOLD[7])
            t.px(ox + 1, oy + 1, GOLD[7])
            t.px(ox + 3, oy + 2, GOLD[3]); t.px(ox + 2, oy + 3, GOLD[2])
            t.px(ox + 2, oy + 2, GOLD[4])
    t.bevel(0, 0, 15, 15, GOLD[4], GOLD[0])
    # Two pixels the old texture hid here and nobody was asked to remove: one
    # from the ATM's screen, one from the server shop's amethyst.
    t.px(5, 10, "#5BE8DA")
    t.px(10, 5, "#843EBA")
    return t


TEXTURES = {
    "block/shop_side.png": shop_side,
    "block/shop_top.png": shop_top,
    "block/shop_front.png": shop_front,
    "block/server_shop_side.png": server_shop_side,
    "block/server_shop_top.png": server_shop_top,
    "block/server_shop_front.png": server_shop_front,
    "block/coin_block.png": coin_block,
    "item/coin.png": coin,
}

if __name__ == "__main__":
    for name, fn in TEXTURES.items():
        fn().save(ASSETS / name)
        print(f"  {name}")
