#!/usr/bin/env python3
"""Builds the Play Store images from the raw emulator captures in store/raw/.

Writes store/screenshots/01-…08-*.png (1080x1920), store/feature-graphic.png (1024x500) and
store/icon-512.png (the launcher icon, full-bleed square; Play rounds the corners itself) and
store/icon-badge-512.png (the Tiger badge icon).
Each image is laid out as HTML and rendered with headless Chrome, then flattened to 24-bit PNG
(Play rejects images with an alpha channel). Needs Google Chrome and ffmpeg.

Raw captures are 1080x2400 phone screenshots. Only rows 0..SCREEN_ROWS are shown, which keeps
the debug build's test-ad banner out of the picture.
"""
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "store" / "raw"
DESIGN = ROOT / "design"
OUT = ROOT / "store"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
SCREEN_ROWS = 1960

TIGER = ("#9C4A00", "#C96A1E")
MEADOW = ("#2F5A29", "#4E8A44")
SKY = ("#1F4F70", "#3B7CA8")
NIGHT = ("#140D08", "#3A2A1E")

SHOTS = [
    ("01-classic", "game.png", TIGER, "Classic tiger &amp; goat strategy", "The traditional board game from Nepal"),
    ("02-vs-ai", "vsai_setup.png", MEADOW, "Play as Tigers or Goats", "Beat the AI on Easy, Medium or Hard"),
    ("03-moves", "select.png", SKY, "Tap a piece to see its moves", "Legal steps and captures are highlighted"),
    ("04-online", "online.png", TIGER, "Play online with anyone", "Quick match, or a 4-digit code for a friend"),
    ("05-win", "result.png", MEADOW, "Capture 5 goats to win", "Or play the goats and trap all four tigers"),
    ("06-how-to-play", "howto.png", SKY, "Learn the rules in a minute", "Illustrated How to Play guide"),
    ("07-dark", "dark_game.png", NIGHT, "Easy on the eyes at night", "Light and dark themes"),
    ("08-resume", "home.png", TIGER, "Games save automatically", "Pick up right where you left off"),
]

FONT = "-apple-system, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif"


def screenshot_html(raw: str, colors: tuple, title: str, subtitle: str) -> str:
    screen_w = 812
    scale = screen_w / 1080
    screen_h = round(SCREEN_ROWS * scale)
    bezel = 16
    top = 1920 - screen_h - bezel
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1080px;height:1920px;overflow:hidden}}
body{{background:linear-gradient(160deg,{colors[1]} 0%,{colors[0]} 70%);font-family:{FONT};color:#fff;position:relative}}
.flags{{position:absolute;left:60px;top:34px;width:960px;opacity:.95}}
.cap{{position:absolute;left:70px;right:70px;top:150px;text-align:center}}
h1{{margin:0;font-size:76px;line-height:1.08;font-weight:800;letter-spacing:-1px}}
p{{margin:22px 0 0;font-size:40px;line-height:1.25;font-weight:500;opacity:.9}}
.phone{{position:absolute;left:{(1080 - screen_w) // 2 - bezel}px;top:{top}px;width:{screen_w}px;
  height:{screen_h + 200}px;padding:{bezel}px;background:#14100d;border-radius:72px 72px 0 0;
  box-shadow:0 30px 80px rgba(0,0,0,.45)}}
.screen{{width:{screen_w}px;height:{screen_h + 200}px;border-radius:58px 58px 0 0;overflow:hidden;background:#fff}}
.screen img{{width:{screen_w}px;display:block}}
</style></head><body>
<img class="flags" src="{(DESIGN / 'prayer_flags.svg').as_uri()}">
<div class="cap"><h1>{title}</h1><p>{subtitle}</p></div>
<div class="phone"><div class="screen"><img src="{(RAW / raw).as_uri()}"></div></div>
</body></html>"""


def piece(svg: str, col: int, row: int, board_px: int) -> str:
    unit = board_px / 600
    size = 90 * unit
    x = (75 + 112.5 * col) * unit - size / 2
    y = (75 + 112.5 * row) * unit - size / 2
    return f'<img src="{(DESIGN / svg).as_uri()}" style="position:absolute;left:{x:.1f}px;top:{y:.1f}px;width:{size:.1f}px">'


def feature_html() -> str:
    mountains = (DESIGN / "mountains.svg").read_text().replace("<svg ", '<svg preserveAspectRatio="none" ', 1)
    board = 400
    tigers = [(0, 0), (4, 0), (2, 2), (4, 4)]
    goats = [(1, 0), (2, 1), (0, 2), (1, 2), (3, 3), (0, 4), (2, 4), (3, 1)]
    pieces = "".join(piece("tiger_piece.svg", c, r, board) for c, r in tigers)
    pieces += "".join(piece("goat_piece.svg", c, r, board) for c, r in goats)
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1024px;height:500px;overflow:hidden}}
body{{background:linear-gradient(120deg,#FFF8F3 0%,#FFE6D2 55%,#FFD3B0 100%);font-family:{FONT};position:relative}}
.mtn{{position:absolute;left:0;bottom:-4px;width:1024px;height:170px;
}}
.mtn svg{{width:100%;height:100%;display:block}}
.flags{{position:absolute;left:24px;top:18px;width:520px}}
.text{{position:absolute;left:60px;top:150px;width:500px}}
h1{{margin:0;font-size:96px;line-height:1;font-weight:900;color:#9C4A00;letter-spacing:-2px;white-space:nowrap}}
h2{{margin:14px 0 0;font-size:42px;font-weight:700;color:#3B2A1E}}
.tags{{margin-top:26px;display:flex;gap:10px}}
.tags span{{font-size:22px;font-weight:700;padding:9px 16px;border-radius:999px;color:#fff}}
.board{{position:absolute;left:574px;top:50px;width:{board}px;height:{board}px;transform:rotate(-4deg);
  filter:drop-shadow(0 18px 30px rgba(80,35,0,.35))}}
.board>img.b{{width:{board}px;height:{board}px;display:block}}
</style></head><body>
<div class="mtn">{mountains}</div>
<img class="flags" src="{(DESIGN / 'prayer_flags.svg').as_uri()}">
<div class="text"><h1>Bagh Chal</h1><h2>Tiger &amp; Goat Game</h2>
<div class="tags"><span style="background:#9C4A00">vs AI</span><span style="background:#2E6386">Online</span><span style="background:#3B6934">2 Players</span></div></div>
<div class="board"><img class="b" src="{(DESIGN / 'board.svg').as_uri()}">{pieces}</div>
</body></html>"""


def icon_html(background: str = "ic_launcher_background.svg", foreground: str = "ic_launcher_foreground.svg") -> str:
    # The launcher shows the middle 72 of the adaptive icon's 108 units; show the same part.
    full = 512 * 108 / 72
    offset = (full - 512) / 2
    layers = "".join(
        f'<img src="{(DESIGN / name).as_uri()}" style="position:absolute;left:-{offset}px;top:-{offset}px;width:{full}px">'
        for name in (background, foreground)
    )
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:512px;height:512px;overflow:hidden;position:relative}}</style></head><body>{layers}</body></html>"""


def render(html: str, width: int, height: int, out: pathlib.Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        page = pathlib.Path(tmp) / "page.html"
        page.write_text(html)
        shot = pathlib.Path(tmp) / "shot.png"
        subprocess.run([
            CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--allow-file-access-from-files",
            "--force-device-scale-factor=1", f"--window-size={width},{height}", f"--screenshot={shot}",
            "--virtual-time-budget=3000", page.as_uri(),
        ], check=True, capture_output=True)
        out.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(shot), "-pix_fmt", "rgb24", str(out)], check=True)
    print(out.relative_to(ROOT))


def main() -> None:
    for name, raw, colors, title, subtitle in SHOTS:
        render(screenshot_html(raw, colors, title, subtitle), 1080, 1920, OUT / "screenshots" / f"{name}.png")
    render(feature_html(), 1024, 500, OUT / "feature-graphic.png")
    render(icon_html(), 512, 512, OUT / "icon-512.png")
    render_badge_icon()


def render_badge_icon() -> None:
    """The Tiger badge app icon, ready to swap in as the Play listing icon."""
    render(icon_html("launcher_badge_background.svg", "launcher_badge_foreground.svg"), 512, 512, OUT / "icon-badge-512.png")


if __name__ == "__main__":
    main()
