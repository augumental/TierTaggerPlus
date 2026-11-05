#!/usr/bin/env python3
"""Generates every icon texture, the icon font and the Java icon map for TierTagger+.

MCTiers + SubTiers icons come from uku3lig/TierTagger (MPL-2.0), PvPTiers icons from
PvPTiers/Tiers (GPL-3.0). StatTiers, TierTests, MSM, PvPHQ and MCPVP icons are original
pixel art drawn below, each framed in its own tile shape/colour so the lists are easy to
tell apart at nametag size.

usage: gen_icons.py <uku3lig textures dir> <pvptiers resourcepack minecraft dir> <project root>
"""
import json
import os
import sys
from PIL import Image, ImageDraw

UKU, PVP, ROOT = sys.argv[1], sys.argv[2], sys.argv[3]
ASSETS = os.path.join(ROOT, "src/client/resources/assets/tiertaggerplus")
TEX = os.path.join(ASSETS, "textures/icon")
os.makedirs(TEX, exist_ok=True)
os.makedirs(os.path.join(ASSETS, "font"), exist_ok=True)

# --------------------------------------------------------------------------------------
# 12x12 glyphs. '.' = transparent; other chars map through the glyph palette.
# --------------------------------------------------------------------------------------
G = {}
P = {}

def glyph(name, rows, pal):
    assert len(rows) <= 12, name
    rows = rows + ["." * 12] * (12 - len(rows))
    for r in rows:
        assert len(r) == 12, (name, r)
    G[name] = rows
    P[name] = pal

glyph("sword", [
    "..........WW",
    ".........WLW",
    "........WLW.",
    ".......WLW..",
    "......WLW...",
    ".....WLW....",
    ".G..WLW.....",
    ".GG.LW......",
    "..GGG.......",
    "..HGG.......",
    ".HH..G......",
    "HH..........",
], {"W": "#eafcff", "L": "#5fd0ef", "G": "#d9a92b", "H": "#6b4423"})

glyph("axe", [
    ".....SSSS...",
    "....SWWWSS..",
    "....SWWWWS..",
    ".....SWWSH..",
    "......SSHH..",
    ".......HH...",
    "......HH....",
    ".....HH.....",
    "....HH......",
    "...HH.......",
    "..HH........",
    ".HH.........",
], {"S": "#5e6670", "W": "#cfd8e0", "H": "#9a6533"})

glyph("mace", [
    "....D.D.D...",
    "...DSSSSSD..",
    "..DSWWSSSSD.",
    "...SWSSSSS..",
    "..DSSSSSSSD.",
    "...SSSSSSS..",
    "..DSSSSSSD..",
    "...D.SHSD...",
    ".....HH.....",
    "....HH......",
    "...HH.......",
    "..HH........",
], {"D": "#3d434c", "S": "#8b939e", "W": "#e1e7ee", "H": "#6b4423"})

glyph("potion", [
    ".....CC.....",
    "....CCCC....",
    ".....GG.....",
    ".....GG.....",
    "....G..G....",
    "...G.RR.G...",
    "..G.RRRR.G..",
    "..GRRWRRRG..",
    "..GRRRRRRG..",
    "..GRRRRRDG..",
    "...GRRRDG...",
    "....GGGG....",
], {"G": "#d5e6f0", "C": "#8a5a2b", "R": "#e8304a", "W": "#ffb7c0", "D": "#a81d33"})

glyph("heart", [
    "............",
    "..DDD..DDD..",
    ".DWWRDDRRRD.",
    ".DWRRRRRRRD.",
    ".DRRRRRRRRD.",
    ".DRRRRRRRRD.",
    "..DRRRRRRD..",
    "...DRRRRD...",
    "....DRRD....",
    ".....DD.....",
], {"R": "#e4252f", "W": "#ffa2a8", "D": "#8e1119"})

glyph("pearl", [
    "............",
    "....DDDD....",
    "...DTTTTD...",
    "..DTLLTTTD..",
    "..DTLTTTTD..",
    "..DTTKKTTD..",
    "..DTTKKTTD..",
    "..DTTTTTMD..",
    "...DTTTMD...",
    "....DDDD....",
], {"D": "#0b3b3a", "T": "#239d8f", "L": "#a8f5e4", "M": "#12665e", "K": "#0a2c2a"})

glyph("crystal", [
    ".....PP.....",
    "....PWWP....",
    "...PWPPPP...",
    "..PWPPPPKP..",
    ".PPPPPPPPKP.",
    "..PPPPPPKP..",
    "...PPPPKP...",
    "....PPKP....",
    ".....PP.....",
    "...OOOOOO...",
    "..OLLOOOOO..",
    "..OOOOOOOO..",
], {"P": "#d36bff", "W": "#f7dcff", "K": "#9132c9", "O": "#2c2038", "L": "#56406e"})

glyph("diamond", [
    "............",
    "............",
    "...CCCCCC...",
    "..CWWCLLLC..",
    ".CWLLCLLLLC.",
    ".CCCCCCCCCC.",
    "..CLLLLLLC..",
    "...CLLLLC...",
    "....CLLC....",
    ".....CC.....",
], {"C": "#169db3", "L": "#6ff0f5", "W": "#e4ffff"})

glyph("cart", [
    "............",
    "............",
    "............",
    ".GGGGGGGGGG.",
    ".GLLLLLLLLG.",
    ".GDDDDDDDDG.",
    ".GDDDDDDDDG.",
    "..GGGGGGGG..",
    "..OO....OO..",
    "..OO....OO..",
], {"G": "#9aa3ad", "L": "#d6dde4", "D": "#59616a", "O": "#22262b"})

glyph("bow", [
    "....HHH.....",
    "..HH...S....",
    "..H....S....",
    ".H.....S....",
    ".H.....S..F.",
    ".H.AAAAAAAAT",
    ".H.....S..F.",
    ".H.....S....",
    "..H....S....",
    "..HH...S....",
    "....HHH.....",
], {"H": "#8a5a2b", "S": "#eeeeee", "A": "#c9a978", "T": "#c3ced6", "F": "#ffffff"})

glyph("bed", [
    "............",
    "............",
    "............",
    ".WWW........",
    ".WWWRRRRRRR.",
    ".WWWRRRRRRR.",
    "LLLLLLLLLLLL",
    "PPPPPPPPPPPP",
    "P..........P",
    "P..........P",
], {"W": "#f4f4f4", "R": "#c62a2a", "L": "#e7e1d3", "P": "#7a5230"})

glyph("creeper", [
    "GGGGGGGGGGGG",
    "GLGGGGGGGLGG",
    "GGKKKGGKKKGG",
    "GGKKKGGKKKGG",
    "GGKKKGGKKKGG",
    "GGGGGKKGGGGG",
    "GGGGKKKKGGGG",
    "GGGKKKKKKGGG",
    "GGGKKGGKKGGG",
    "GGGKKGGKKGGG",
    "GLGGGGGGGGLG",
    "GGGGGGGGGGGG",
], {"G": "#4fbf4f", "L": "#94e594", "K": "#0f2e0f"})

glyph("trident", [
    ".W...W...W..",
    ".T...T...T..",
    ".TT..T..TT..",
    "..TTTTTTT...",
    ".....T......",
    ".....T......",
    ".....D......",
    ".....T......",
    ".....T......",
    ".....D......",
    ".....T......",
    ".....D......",
], {"T": "#5fd3b8", "W": "#d8fff4", "D": "#2f8a74"})

glyph("elytra", [
    "............",
    "..LL....LL..",
    ".LEEL..LEEL.",
    ".LEEELLEEEL.",
    "LEEEEEEEEEEL",
    "LEEEDEEDEEEL",
    "LEEDDEEDDEEL",
    ".LEDD..DDEL.",
    ".LDD....DDL.",
    ".LD......DL.",
    "..L......L..",
], {"E": "#a3a3d2", "L": "#55558a", "D": "#7a7aaa"})

glyph("spear", [
    "..........WW",
    ".........WLW",
    "........LLW.",
    ".......HLL..",
    "......HH....",
    ".....HH.....",
    "....HH......",
    "...HH.......",
    "..RH........",
    ".RRR........",
    "RR.R........",
    "R...........",
], {"W": "#eef4fa", "L": "#9fb0c0", "H": "#7a5230", "R": "#c43a3a"})

glyph("shield", [
    ".DDDDDDDDDD.",
    ".DWWWWWWWWD.",
    ".DWLWWWWWWD.",
    ".DWWWDDWWWD.",
    ".DWWDDDDWWD.",
    ".DWWWDDWWWD.",
    ".DWWWWWWWWD.",
    "..DWWWWWWD..",
    "..DWWWWWWD..",
    "...DWWWWD...",
    "....DDDD....",
], {"W": "#b88a4a", "L": "#e0b878", "D": "#585e66"})

glyph("bolt", [
    "........YY..",
    ".......YWY..",
    "......YWY...",
    ".....YWY....",
    "....YWY.....",
    "...YYYYYYY..",
    ".......YWY..",
    "......YWY...",
    ".....YWY....",
    "....YWY.....",
    "...YY.......",
    "...Y........",
], {"Y": "#46b8ec", "W": "#d9f5ff"})

glyph("soup", [
    "............",
    "............",
    "...R...R....",
    "..RRR.RRR...",
    "...S...S....",
    ".SSSSSSSSSS.",
    ".BBBBBBBBBB.",
    ".BWBBBBBBBB.",
    "..BBBBBBBB..",
    "...BBBBBB...",
    "....BBBB....",
], {"S": "#c98a5a", "B": "#7a4f2b", "W": "#a67447", "R": "#d23a3a"})

glyph("fireball", [
    ".......O....",
    "....O.OO....",
    "...OOOYO.O..",
    "..OOYYYYOO..",
    ".OOYYWWYYOO.",
    ".OYYWWWWYYO.",
    ".OYYWWWWYYO.",
    ".OOYYWWYYOO.",
    "..OOYYYYOO..",
    "...OOOOOO...",
    "....OOOO....",
], {"O": "#e0541b", "Y": "#ffb22e", "W": "#fff2a8"})

glyph("glove", [
    "............",
    "...RRRRR....",
    "..RRWRRRRR..",
    ".RRWRRRRRRR.",
    ".RRRRRRRRRR.",
    ".RRRRRRRRDR.",
    ".DRRRRRRRDR.",
    "..DRRRRRRD..",
    "...LLLLLL...",
    "...LLLLLL...",
], {"R": "#d6283a", "W": "#ff8e9a", "D": "#8f1422", "L": "#f2f2f2"})

glyph("sumo", [
    "....RRRR....",
    "..RR....RR..",
    ".R........R.",
    ".R..B..C..R.",
    "R..BBB.CCC.R",
    "R...B...C..R",
    "R..B.B.C.C.R",
    ".R........R.",
    "..RR....RR..",
    "....RRRR....",
], {"R": "#e8d7a8", "B": "#ff5b5b", "C": "#5b8bff"})

glyph("bridge", [
    "............",
    "............",
    ".R........B.",
    ".RR......BB.",
    ".R.PPPPPP.B.",
    "WWWWWWWWWWWW",
    "DDDDDDDDDDDD",
    ".D..D..D..D.",
    ".D..D..D..D.",
    ".D..D..D..D.",
], {"W": "#c08a50", "D": "#6e4a2a", "P": "#dcc490", "R": "#e04848", "B": "#4878e0"})

glyph("rod", [
    "..........HH",
    ".........HH.",
    "........HH.S",
    ".......HH..S",
    "......HH...S",
    ".....HH....S",
    "....HH.....S",
    "...HH......S",
    "..HH......SK",
    ".HH........K",
    "HH.......KK.",
], {"H": "#7a5230", "S": "#dddddd", "K": "#9aa6b0"})

glyph("rocket", [
    ".....RR.....",
    "....RRRR....",
    "....RWRR....",
    "....RRRR....",
    "....PPPP....",
    "....RRRR....",
    "....RWRR....",
    "....RRRR....",
    ".....SS.....",
    ".Y...SS...Y.",
    "..Y.S..S.Y..",
    "...Y....Y...",
], {"R": "#d63030", "W": "#ff9a9a", "P": "#f2f2f2", "S": "#8a8a8a", "Y": "#ffcf3a"})

glyph("ingot", [
    "............",
    "............",
    "............",
    "....LLLLLL..",
    "...LWWWWWL..",
    "..LWWWWWLD..",
    ".LMMMMMMLDD.",
    ".LMMMMMMLD..",
    ".LMMMMMMD...",
    "..DDDDDD....",
], {"W": "#fff3a0", "L": "#e8c33a", "M": "#d9a521", "D": "#9c6d10"})

glyph("gapple", [
    ".....S......",
    ".....SLL....",
    "....S.LL....",
    "..YYYYYYY...",
    ".YWYYYYYYY..",
    ".YWYYYYYYY..",
    ".YYYYYYYYY..",
    ".YYYYYYYYD..",
    "..YYYYYYD...",
    "...YYDDD....",
], {"Y": "#f2c53a", "W": "#fff2a0", "D": "#b88a12", "S": "#6b4423", "L": "#4caf50"})

glyph("sun", [
    ".....YY.....",
    "..Y..YY..Y..",
    "...Y....Y...",
    "....OOOO....",
    "...OYYYYO...",
    "YY.OYWWYO.YY",
    "YY.OYWYYO.YY",
    "...OYYYYO...",
    "....OOOO....",
    "...Y....Y...",
    "..Y..YY..Y..",
    ".....YY.....",
], {"Y": "#ffd23a", "O": "#f08a1c", "W": "#fff6c0"})

glyph("moon", [
    "......LLLL..",
    "....LLMM....",
    "...LMM......",
    "..LMM.......",
    "..LMM.....S.",
    ".LMMM.......",
    ".LMMM.......",
    "..LMMM......",
    "..LMMMM...L.",
    "...LMMMMMML.",
    "....LLMMML..",
    "......LLL...",
], {"M": "#d6deff", "L": "#7f8cc4", "S": "#ffffff"})

glyph("endeye", [
    "....GGGG....",
    "..GGTTTTGG..",
    ".GTTTTTTTTG.",
    ".GTTKKKKTTG.",
    "GTTKKYYKKTTG",
    "GTTKYYYYKTTG",
    "GTTKKYYKKTTG",
    ".GTTKKKKTTG.",
    ".GTTTTTTTTG.",
    "..GGTTTTGG..",
    "....GGGG....",
], {"G": "#1e5a3a", "T": "#3fbf7a", "K": "#0b2a17", "Y": "#b6ff5a"})

glyph("compass", [
    "....DDDD....",
    "..DDGGGGDD..",
    ".DGGGGRGGGD.",
    ".DGGGGRGGGD.",
    "DGGGGRRGGGGD",
    "DGGGGWGGGGGD",
    "DGGGGBGGGGGD",
    ".DGGGBGGGGD.",
    ".DGGGGGGGGD.",
    "..DDGGGGDD..",
    "....DDDD....",
], {"G": "#9097a1", "D": "#454a52", "R": "#e03030", "W": "#ffffff", "B": "#2e2e2e"})

glyph("star", [
    ".....YY.....",
    ".....YY.....",
    "....YWWY....",
    "YYYYYWYYYYYY",
    ".YYYYYYYYYY.",
    "..YYYYYYYY..",
    "...YYYYYY...",
    "...YYYYYY...",
    "..YYY..YYY..",
    "..YY....YY..",
    ".Y........Y.",
], {"Y": "#ffcf3a", "W": "#fff6c8"})

# palette variants: name -> (base glyph, overrides)
VARIANTS = {
    "nethpot": ("potion", {"R": "#6c3fa8", "W": "#b99be6", "D": "#41226e", "G": "#8d8a96"}),
    "diapot": ("potion", {"R": "#2bc4dd", "W": "#b6f6ff", "D": "#13798a"}),
    "debuff": ("potion", {"R": "#7aa832", "W": "#d2f08c", "D": "#4a6e17"}),
    "nethsword": ("sword", {"W": "#a89aa2", "L": "#4f4249", "G": "#3a3036"}),
    "crystal_dia": ("crystal", {"P": "#4fd8ef", "W": "#d8fbff", "K": "#1e8ea3"}),
    "bolt_op": ("bolt", {"Y": "#ff8a1e", "W": "#fff1c4"}),
    "bed_blue": ("bed", {"R": "#2f6fd8"}),
    "iron": ("ingot", {"W": "#ffffff", "L": "#dadada", "M": "#bcbcbc", "D": "#767676"}),
    "mace_lt": ("mace", {"S": "#6fa8dc", "W": "#d6ecff", "D": "#2e5c86"}),
    "mace_ht": ("mace", {"S": "#e0863a", "W": "#ffd9b0", "D": "#8a4a14"}),
    "mace_og": ("mace", {"S": "#d9b13a", "W": "#fff2b8", "D": "#7d5f10"}),
}

def hexrgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

def render_glyph(name):
    if name in VARIANTS:
        base, ov = VARIANTS[name]
        rows, pal = G[base], dict(P[base]); pal.update(ov)
    else:
        rows, pal = G[name], P[name]
    im = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != ".":
                im.putpixel((x, y), hexrgba(pal[ch]))
    return im

# --------------------------------------------------------------------------------------
# Tile shapes per tierlist (16x16)
# --------------------------------------------------------------------------------------
def mask(shape):
    m = Image.new("L", (16, 16), 0)
    d = ImageDraw.Draw(m)
    if shape == "round":
        d.rounded_rectangle((0, 0, 15, 15), radius=4, fill=255)
    elif shape == "circle":
        d.ellipse((0, 0, 15, 15), fill=255)
    elif shape == "hex":
        d.polygon([(4, 0), (11, 0), (15, 7), (15, 8), (11, 15), (4, 15), (0, 8), (0, 7)], fill=255)
    elif shape == "shield":
        d.polygon([(0, 0), (15, 0), (15, 8), (8, 15), (7, 15), (0, 8)], fill=255)
    elif shape == "cut":
        d.polygon([(3, 0), (12, 0), (15, 3), (15, 12), (12, 15), (3, 15), (0, 12), (0, 3)], fill=255)
    return m

def tile(shape, fill, border):
    m = mask(shape)
    inner = Image.new("L", (16, 16), 0)
    # erode by one pixel for the border
    px = m.load(); ip = inner.load()
    for y in range(16):
        for x in range(16):
            if px[x, y] and all(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy]
                                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                ip[x, y] = 255
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out.paste(Image.new("RGBA", (16, 16), hexrgba(border)), (0, 0), m)
    out.paste(Image.new("RGBA", (16, 16), hexrgba(fill)), (0, 0), inner)
    return out

STYLES = {
    "stattiers": ("round", "#0c1a33", "#38d6ff"),
    "tiertests": ("circle", "#0b2a17", "#3ee07a"),
    "msm": ("hex", "#22113d", "#b07cff"),
    "pvphq": ("shield", "#2e0b10", "#ff4d5e"),
    "mcpvp": ("cut", "#2d1d05", "#ffb43b"),
}

def framed(listid, gname):
    shape, fill, border = STYLES[listid]
    t = tile(shape, fill, border)
    g = render_glyph(gname)
    t.alpha_composite(g, (2, 2) if shape != "shield" else (2, 1))
    return t

def up(im, size=32):
    return im.resize((size, size), Image.NEAREST)

# --------------------------------------------------------------------------------------
# Logos for the 5 new lists (16x16 pixel art, framed like their icons)
# --------------------------------------------------------------------------------------
LOGO_GLYPHS = {
    "stattiers": ([
        "............",
        "..........C.",
        ".........CC.",
        "........C...",
        "...C...C..W.",
        "..C.C.C...W.",
        ".C...C..W.W.",
        "........W.W.",
        ".....W..W.W.",
        "..W..W..W.W.",
        "..W..W..W.W.",
        "............",
    ], {"C": "#38d6ff", "W": "#e8faff"}),
    "tiertests": ([
        "............",
        "...WWWWWW...",
        "....W..W....",
        "....W..W....",
        "...W....W...",
        "..W..GG..W..",
        ".W..GGGG..W.",
        ".WGGGGGGGGW.",
        ".WGGGGLGGGW.",
        ".WGGGGGGGGW.",
        "..WWWWWWWW..",
        "............",
    ], {"W": "#e7fff0", "G": "#3ee07a", "L": "#b9ffd2"}),
    "msm": None,  # uses mace glyph recoloured
    "pvphq": ([
        "............",
        "............",
        ".W..W..WWW..",
        ".W..W.W...W.",
        ".WWWW.W...W.",
        ".W..W.W.W.W.",
        ".W..W.W..W..",
        ".W..W..WW.W.",
        "............",
        "...RRRRRR...",
        "....RRRR....",
        "............",
    ], {"W": "#ffffff", "R": "#ff4d5e"}),
    "mcpvp": ([
        "W..........W",
        ".W........W.",
        "..W......W..",
        "...W....W...",
        "....W..W....",
        ".....WW.....",
        ".....WW.....",
        "....G..G....",
        "...G....G...",
        ".HG......GH.",
        "HH........HH",
        "H..........H",
    ], {"W": "#fff1cc", "G": "#ffb43b", "H": "#8a5a2b"}),
}

def logo(listid):
    shape, fill, border = STYLES[listid]
    t = tile(shape, fill, border)
    spec = LOGO_GLYPHS[listid]
    if spec is None:
        G["_msm_logo"] = G["mace"]
        P["_msm_logo"] = dict(P["mace"], S="#b07cff", W="#ecdcff", D="#5a2e99")
        g = render_glyph("_msm_logo")
    else:
        G["_l_" + listid], P["_l_" + listid] = spec
        g = render_glyph("_l_" + listid)
    t.alpha_composite(g, (2, 2) if shape != "shield" else (2, 1))
    return t

# --------------------------------------------------------------------------------------
# Mode -> icon mapping for every tierlist
# --------------------------------------------------------------------------------------
NEW_LISTS = {
    "stattiers": {
        "axe": "axe", "bed": "bed", "bow": "bow", "creeper": "creeper", "crystal": "crystal",
        "diacrystal": "crystal_dia", "diasmp": "diamond", "diapot": "diapot", "elytra": "elytra",
        "goldogv": "ingot", "ironcrystal": "iron", "mace": "mace", "minecart": "cart",
        "nethsword": "nethsword", "nethpot": "nethpot", "ogv": "gapple", "smp": "pearl",
        "soup": "soup", "spearmace": "spear", "speed": "bolt", "speedop": "bolt_op",
        "sword": "sword", "trident": "trident", "uhc": "heart",
    },
    "tiertests": {
        "bedfight": "bed", "bedwars": "bed_blue", "boxing": "glove", "bridge": "bridge",
        "crystal": "crystal", "fireballfight": "fireball", "mace": "mace", "neth": "nethpot",
        "netherite_pot": "nethpot", "nethpot": "nethpot", "nodebuff": "potion", "smp": "pearl",
        "sumo": "sumo", "sword": "sword", "uhc": "heart", "uhc_modern": "heart",
    },
    "msm": {
        "rod-mace": "rod", "og-mace": "mace_og", "lt-mace": "mace_lt", "rocket-mace": "rocket",
        "ht-mace": "mace_ht", "spear-mace": "spear",
    },
    "pvphq": {
        "sword": "sword", "axe": "axe", "mace": "mace", "spear_mace": "spear", "uhc": "heart",
        "netherite_pot": "nethpot", "pot": "potion", "smp": "pearl", "diamond_smp": "diamond",
        "vanilla": "crystal", "cart": "cart",
    },
    "mcpvp": {
        "sword": "sword", "shield": "shield", "pot": "potion", "early_game": "sun",
        "end_game": "endeye", "mace": "mace", "late_game": "moon", "spear": "spear",
        "diamond_smp": "diamond", "netherite_pot": "nethpot", "creeper": "creeper", "cart": "cart",
        "bow": "bow", "smp": "pearl", "crystal": "crystal",
    },
}

UKU_LISTS = {
    "mctiers": {m: f"mctiers/{m}.png" for m in ["axe", "mace", "nethop", "pot", "smp", "sword", "uhc", "vanilla"]},
    "subtiers": {m: f"subtiers/{m}.png" for m in ["bed", "bow", "creeper", "debuff", "dia_crystal", "dia_smp",
                                                 "elytra", "manhunt", "minecart", "og_vanilla", "speed", "trident"]},
}
PVP_ICONS = {m: f"textures/font/gamemodes/pvptiers/{m}.png"
             for m in ["axe", "crystal", "mace", "neth_pot", "pot", "smp", "sword", "uhc"]}

icons = {}  # "list/mode" -> texture name (without .png)

def save(im, name):
    im.save(os.path.join(TEX, name + ".png"))

def fit32(path):
    im = Image.open(path).convert("RGBA")
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    w, h = im.size
    s = max(w, h)
    sq = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    sq.alpha_composite(im, ((s - w) // 2, (s - h) // 2))
    return sq.resize((32, 32), Image.NEAREST if s <= 32 else Image.LANCZOS)

# existing lists
for lid, modes in UKU_LISTS.items():
    for mode, rel in modes.items():
        n = f"{lid}_{mode}"
        save(fit32(os.path.join(UKU, rel)), n)
        icons[f"{lid}/{mode}"] = n
for mode, rel in PVP_ICONS.items():
    n = f"pvptiers_{mode}"
    save(fit32(os.path.join(PVP, rel)), n)
    icons[f"pvptiers/{mode}"] = n

# new lists
for lid, modes in NEW_LISTS.items():
    for mode, gname in modes.items():
        n = f"{lid}_{gname}"
        if not os.path.exists(os.path.join(TEX, n + ".png")):
            save(up(framed(lid, gname)), n)
        icons[f"{lid}/{mode}"] = n
    # fallback icon for modes added to the list later
    n = f"{lid}_star"
    save(up(framed(lid, "star")), n)
    icons[f"{lid}/*"] = n

# fallbacks for the three existing lists
for lid in ("mctiers", "subtiers", "pvptiers"):
    icons[f"{lid}/*"] = f"{lid}_logo"

# logos (64x64 for GUI headers, also used as small inline icons)
save(fit32(os.path.join(UKU, "tierlists/mctiers.png")).resize((64, 64), Image.LANCZOS), "mctiers_logo")
im = Image.open(os.path.join(UKU, "tierlists/subtiers.png")).convert("RGBA"); save(im.resize((64, 64), Image.LANCZOS), "subtiers_logo")
im = Image.open(os.path.join(PVP, "textures/pvptiers_logo.png")).convert("RGBA"); save(im.resize((64, 64), Image.LANCZOS), "pvptiers_logo")
for lid in NEW_LISTS:
    save(logo(lid).resize((64, 64), Image.NEAREST), f"{lid}_logo")
for lid in ["mctiers", "subtiers", "pvptiers"] + list(NEW_LISTS):
    icons[f"{lid}/@logo"] = f"{lid}_logo"

# --------------------------------------------------------------------------------------
# Font: one glyph per texture, private use area starting at U+E100
# --------------------------------------------------------------------------------------
textures = sorted(set(icons.values()))
codes = {t: 0xE100 + i for i, t in enumerate(textures)}
providers = [{"type": "space", "advances": {"": 1}}]
for t in textures:
    providers.append({"type": "bitmap", "file": f"tiertaggerplus:icon/{t}.png", "ascent": 7, "height": 8,
                      "chars": [chr(codes[t])]})
with open(os.path.join(ASSETS, "font/icons.json"), "w") as f:
    json.dump({"providers": providers}, f, indent=1)

# Java icon map
java_dir = os.path.join(ROOT, "src/client/java/dev/tiertaggerplus/render")
os.makedirs(java_dir, exist_ok=True)
lines = [
    "package dev.tiertaggerplus.render;",
    "",
    "import java.util.HashMap;",
    "import java.util.Map;",
    "",
    "/** GENERATED by tools/gen_icons.py - do not edit. */",
    "final class IconMap {",
    "    /** \"list/mode\" -> texture name */",
    "    static final Map<String, String> TEXTURE = new HashMap<>();",
    "    /** texture name -> font codepoint */",
    "    static final Map<String, Character> CHAR = new HashMap<>();",
    "",
    "    static {",
]
for k in sorted(icons):
    lines.append(f'        TEXTURE.put("{k}", "{icons[k]}");')
for t in textures:
    lines.append(f"        CHAR.put(\"{t}\", (char) 0x{codes[t]:04X});")
lines += ["    }", "", "    private IconMap() {", "    }", "}", ""]
with open(os.path.join(java_dir, "IconMap.java"), "w") as f:
    f.write("\n".join(lines))

# preview sheet for review
rows = [[k for k in sorted(icons) if k.startswith(l + "/")] for l in
        ["mctiers", "subtiers", "pvptiers"] + list(NEW_LISTS)]
S = 40
sheet = Image.new("RGBA", (S * max(len(r) for r in rows) + 8, S * len(rows) + 8), (34, 34, 40, 255))
for y, r in enumerate(rows):
    for x, k in enumerate(r):
        im = Image.open(os.path.join(TEX, icons[k] + ".png")).convert("RGBA").resize((32, 32), Image.NEAREST)
        sheet.alpha_composite(im, (4 + x * S, 4 + y * S))
os.makedirs(os.path.join(ROOT, "tools/out"), exist_ok=True)
sheet.save(os.path.join(ROOT, "tools/out/icon_sheet.png"))
print(f"{len(textures)} textures, {len(icons)} mappings")
