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
"""Source of the three screen backgrounds under assets/ravencoin/textures/gui.

Kept as code for the same reason the block textures are: a panel is a grid of
one-pixel bevels whose only job is to line up with coordinates written in Java,
and a hand edit that is one pixel out is invisible in review and impossible to
reproduce. Here the frame positions and the Java constants can be read side by
side.

The palette is vanilla's, sampled rather than remembered:

    #C6C6C6  panel body          #8B8B8B  slot fill
    #FFFFFF  light bevel         #787878  sunken display fill
    #555555  dark bevel          #373737  dark bevel, inside a sunken edge

Every sunken thing on a Minecraft screen — a slot, a display box — is the same
bevel: dark along the top and left, light along the bottom and right, with the
two opposite corners left as fill so the light and dark edges do not meet.

Run from anywhere:  python3 tools/generate_gui.py
"""
from pathlib import Path

from PIL import Image

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/ravencoin/textures/gui"

# The sheet is 256x256 whatever the panel is; the screen blits the top-left
# corner at the size it declares, and the rest stays transparent.
SHEET = 256

BODY = (0xC6, 0xC6, 0xC6, 255)
LIGHT = (0xFF, 0xFF, 0xFF, 255)
DARK = (0x55, 0x55, 0x55, 255)
SLOT_FILL = (0x8B, 0x8B, 0x8B, 255)
DISPLAY_FILL = (0x78, 0x78, 0x78, 255)
SUNK_EDGE = (0x37, 0x37, 0x37, 255)


class Panel:
    def __init__(self, width, height):
        self.image = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
        self.px = self.image.load()
        self.width = width
        self.height = height

        self.rect(0, 0, width, height, BODY)
        # Raised outer edge: light on top and left, dark on bottom and right,
        # with the two far corners left as body so the edges never meet.
        self.rect(0, 0, width - 1, 1, LIGHT)
        self.rect(0, 0, 1, height - 1, LIGHT)
        self.rect(1, height - 1, width - 1, 1, DARK)
        self.rect(width - 1, 1, 1, height - 1, DARK)

    def rect(self, x, y, w, h, colour):
        for row in range(y, y + h):
            for column in range(x, x + w):
                self.px[column, row] = colour

    def sunken(self, x, y, w, h, fill):
        """A recessed area — the shape every slot and every display box is."""
        self.rect(x, y, w, h, fill)
        self.rect(x, y, w - 1, 1, SUNK_EDGE)
        self.rect(x, y, 1, h - 1, SUNK_EDGE)
        self.rect(x + 1, y + h - 1, w - 1, 1, LIGHT)
        self.rect(x + w - 1, y + 1, 1, h - 1, LIGHT)

    def slot(self, x, y):
        self.sunken(x, y, 18, 18, SLOT_FILL)

    def display(self, x, y, w, h):
        self.sunken(x, y, w, h, DISPLAY_FILL)

    def inventory(self, top):
        """The player's own inventory, at the offset the menu class passes.

        Menus place items at ``8 + column * 18``, so the frame around one starts
        a pixel earlier. The hotbar sits four pixels below the pack rather than
        flush against it, which is vanilla's spacing and the reason a player can
        tell the two apart at a glance.
        """
        for row in range(3):
            for column in range(9):
                self.slot(7 + column * 18, top - 1 + row * 18)
        for column in range(9):
            self.slot(7 + column * 18, top + 57)

    def save(self, name):
        self.image.save(GUI / name)
        print("  %-18s %d x %d" % (name, self.width, self.height))


def atm():
    """The bank terminal.

    No inventory: this screen is a menu and its pages, and the coins it moves
    are counted for you on the display rather than shown as slots. The content
    area below the display runs from y 56 to y 172, with the footer row of
    buttons at 178 — which is what the page layouts in AtmScreen are written
    against.
    """
    panel = Panel(176, 222)
    panel.display(7, 17, 162, 32)
    return panel


def shop():
    """The buying screen.

    Two slot frames rather than one. What is sold and what it costs are both
    items, so both get shown as items — a price whose name is thirty characters
    long is a picture in one place and an overflow in the other.
    """
    panel = Panel(176, 196)
    panel.slot(7, 28)
    panel.slot(91, 28)
    panel.inventory(114)
    return panel


def shop_config():
    """The owner's screen.

    Same two frames as the buying screen, in the same places, on purpose. Twenty
    pixels taller than it used to be, for the row that restocks a market stall
    and puts a server shop on the market.
    """
    panel = Panel(176, 236)
    panel.slot(7, 28)
    panel.slot(91, 28)
    panel.inventory(154)
    return panel


PANELS = {
    "atm.png": atm,
    "shop.png": shop,
    "shop_config.png": shop_config,
}

if __name__ == "__main__":
    for name, build in PANELS.items():
        build().save(name)
