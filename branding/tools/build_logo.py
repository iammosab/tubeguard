"""Builds the TubeGuard logo masters: the seal symbol (regular and small-size cut), the wordmark as outlines,
and the horizontal and stacked lockups. Run with a Python that has fontTools; pass the Plus Jakarta Sans
variable font path."""
import math
import sys
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

OUT = Path(__file__).resolve().parent.parent / "masters"
OUT.mkdir(exist_ok=True)


def f(x):
    return ("%.2f" % x).rstrip("0").rstrip(".")


def seal(cx, cy, outer, inner, lobes, tension=0.36):
    """A cookie: a smooth wave through `lobes` peaks (radius `outer`) and the valleys between (radius `inner`)."""
    pts = []
    for i in range(2 * lobes):
        a = -math.pi / 2 + i * math.pi / lobes
        r = outer if i % 2 == 0 else inner
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a), a, r))
    d = [f"M{f(pts[0][0])} {f(pts[0][1])}"]
    for i, (x, y, a, r) in enumerate(pts):
        nx, ny, na, nr = pts[(i + 1) % len(pts)]
        t1 = r * (math.pi / lobes) * tension
        t2 = nr * (math.pi / lobes) * tension
        d.append(
            f"C{f(x - t1 * math.sin(a))} {f(y + t1 * math.cos(a))} "
            f"{f(nx + t2 * math.sin(na))} {f(ny - t2 * math.cos(na))} {f(nx)} {f(ny)}"
        )
    return " ".join(d) + " Z"


def play(cx, cy, width, radius):
    """A play triangle with 30-degree edges and softly rounded corners, centred on its centroid."""
    h = width * math.tan(math.radians(30))
    x0 = cx - width / 3  # the centroid of the triangle sits a third of the way in
    corners = [(x0, cy - h), (x0 + width, cy), (x0, cy + h)]
    d = []
    for i, (x, y) in enumerate(corners):
        px, py = corners[i - 1]
        nx, ny = corners[(i + 1) % 3]

        def toward(ax, ay, bx, by, dist):
            l = math.hypot(bx - ax, by - ay)
            return ax + (bx - ax) * dist / l, ay + (by - ay) * dist / l

        s = toward(x, y, px, py, radius)
        e = toward(x, y, nx, ny, radius)
        d.append(("M" if i == 0 else "L") + f"{f(s[0])} {f(s[1])}")
        d.append(f"Q{f(x)} {f(y)} {f(e[0])} {f(e[1])}")
    return " ".join(d) + " Z"


def symbol_path(small=False):
    if small:
        # Small-size cut: fewer, deeper lobes and a bigger play, so both survive 16-24 px.
        return seal(128, 128, 116, 96, 8, 0.40) + " " + play(131, 128, 92, 6)
    return seal(128, 128, 110, 94, 9) + " " + play(131, 128, 78, 5)


def svg(viewbox, body, title="TubeGuard"):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}"><title>{title}</title>{body}</svg>\n'


def wordmark(font_path, weight=750, tracking=-0.012):
    """'TubeGuard' as outlines. Returns (path data, width, cap height) in font units scaled to cap height 100."""
    font = TTFont(font_path)
    inst = instantiateVariableFont(font, {"wght": weight})
    gs = inst.getGlyphSet()
    cmap = inst.getBestCmap()
    upm = inst["head"].unitsPerEm
    cap = inst["OS/2"].sCapHeight or upm * 0.7
    scale = 100 / cap
    hmtx = inst["hmtx"]
    x = 0.0
    parts = []
    for ch in "TubeGuard":
        name = cmap[ord(ch)]
        pen = SVGPathPen(gs)
        # Flip y (fonts are y-up) and scale so the cap height is 100.
        gs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x, 100)))
        parts.append(pen.getCommands())
        x += hmtx[name][0] * scale + tracking * upm * scale
    return " ".join(parts), x - tracking * upm * scale, 100


def main(font_path):
    sym = symbol_path()
    small = symbol_path(small=True)
    ink = '<path fill="#000" fill-rule="evenodd" d="{}"/>'
    (OUT / "tubeguard-symbol.svg").write_text(svg("0 0 256 256", ink.format(sym)))
    (OUT / "tubeguard-symbol-small.svg").write_text(svg("0 0 256 256", ink.format(small)))

    word, width, cap = wordmark(font_path)
    # Wordmark alone, cap height 100 on a 140-high canvas (room for the descender-free word plus margins).
    (OUT / "tubeguard-wordmark.svg").write_text(
        svg(f"0 -20 {f(width)} 140", f'<path fill="#000" d="{word}"/>')
    )

    # Horizontal lockup: symbol 256 high; the word's cap height is 42% of the symbol, centred on it;
    # the gap is a third of the symbol (the clear-space unit).
    k = 256 * 0.42 / cap
    gap = 256 / 3
    wx = 256 + gap
    wy = 128 - cap * k / 2
    total = wx + width * k
    (OUT / "tubeguard-horizontal.svg").write_text(
        svg(
            f"0 0 {f(total)} 256",
            ink.format(sym) + f'<path fill="#000" transform="translate({f(wx)} {f(wy)}) scale({f(k)})" d="{word}"/>',
        )
    )

    # Stacked lockup: symbol on top, word below, centred; the word is as wide as 1.2x the symbol.
    k2 = 256 * 1.2 / width
    sw = width * k2
    total_w = max(sw, 256)
    sx = (total_w - 256) / 2
    wy2 = 256 + 256 * 0.22
    total_h = wy2 + cap * k2
    (OUT / "tubeguard-stacked.svg").write_text(
        svg(
            f"0 0 {f(total_w)} {f(total_h)}",
            f'<g transform="translate({f(sx)} 0)">{ink.format(sym)}</g>'
            f'<path fill="#000" transform="translate({f((total_w - sw) / 2)} {f(wy2)}) scale({f(k2)})" d="{word}"/>',
        )
    )
    print("masters written to", OUT)


if __name__ == "__main__":
    main(sys.argv[1])
