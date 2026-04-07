from __future__ import annotations

from pathlib import Path
from typing import Iterable, Tuple

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "game-android" / "src" / "main" / "res" / "drawable-nodpi"
SIZE = 256


def rgba(hex_color: str, alpha: int = 255) -> Tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def canvas() -> Image.Image:
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def centered_box(radius: int) -> Tuple[int, int, int, int]:
    return (SIZE // 2 - radius, SIZE // 2 - radius, SIZE // 2 + radius, SIZE // 2 + radius)


def paste_add(base: Image.Image, overlay: Image.Image) -> None:
    base.alpha_composite(overlay)


def glow_ring(radius: int, color: Tuple[int, int, int, int], width: int, blur: int, alpha: int) -> Image.Image:
    layer = canvas()
    draw = ImageDraw.Draw(layer)
    box = centered_box(radius)
    draw.ellipse(box, outline=(color[0], color[1], color[2], alpha), width=width)
    return layer.filter(ImageFilter.GaussianBlur(blur))


def glow_polygon(points: Iterable[Tuple[int, int]], color: Tuple[int, int, int, int], blur: int, alpha: int) -> Image.Image:
    layer = canvas()
    draw = ImageDraw.Draw(layer)
    draw.polygon(list(points), fill=(color[0], color[1], color[2], alpha))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def line_glow(points: Iterable[Tuple[int, int]], color: Tuple[int, int, int, int], width: int, blur: int, alpha: int) -> Image.Image:
    layer = canvas()
    draw = ImageDraw.Draw(layer)
    draw.line(list(points), fill=(color[0], color[1], color[2], alpha), width=width, joint="curve")
    return layer.filter(ImageFilter.GaussianBlur(blur))


def save(img: Image.Image, name: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")


def draw_ship_striker() -> None:
    base = canvas()
    accent = rgba("#5CE7FF")
    hot = rgba("#D2FBFF")
    paste_add(base, glow_polygon([(128, 34), (196, 196), (128, 162), (60, 196)], accent, 16, 180))
    draw = ImageDraw.Draw(base)
    draw.polygon([(128, 42), (188, 186), (128, 150), (68, 186)], fill=accent)
    draw.polygon([(128, 72), (154, 154), (128, 136), (102, 154)], fill=hot)
    draw.rectangle((116, 164, 140, 216), fill=rgba("#FFB85A", 220))
    paste_add(base, glow_ring(74, accent, 8, 10, 140))
    save(base, "fx_ship_striker")


def draw_ship_warden() -> None:
    base = canvas()
    accent = rgba("#88FFC8")
    hot = rgba("#E9FFF6")
    paste_add(base, glow_polygon([(128, 34), (198, 92), (178, 206), (78, 206), (58, 92)], accent, 18, 185))
    draw = ImageDraw.Draw(base)
    draw.rounded_rectangle((72, 70, 184, 188), radius=28, fill=accent)
    draw.rectangle((112, 48, 144, 214), fill=rgba("#153A2E", 210))
    draw.rounded_rectangle((94, 92, 162, 154), radius=16, fill=hot)
    paste_add(base, glow_ring(82, accent, 10, 12, 145))
    save(base, "fx_ship_warden")


def draw_ship_specter() -> None:
    base = canvas()
    accent = rgba("#AAB5FF")
    hot = rgba("#F6F7FF")
    paste_add(base, glow_polygon([(128, 28), (210, 112), (166, 210), (90, 210), (46, 112)], accent, 20, 175))
    draw = ImageDraw.Draw(base)
    draw.polygon([(128, 40), (196, 112), (160, 198), (96, 198), (60, 112)], fill=accent)
    draw.ellipse((102, 90, 154, 142), fill=hot)
    draw.line((74, 114, 182, 114), fill=rgba("#E5EAFF", 220), width=10)
    paste_add(base, glow_ring(80, accent, 8, 12, 130))
    save(base, "fx_ship_specter")


def draw_enemy_core(name: str, body: str, inner: str, shape: str) -> None:
    base = canvas()
    accent = rgba(body)
    paste_add(base, glow_ring(62, accent, 18, 16, 140))
    draw = ImageDraw.Draw(base)
    if shape == "circle":
        draw.ellipse((66, 66, 190, 190), fill=accent)
    elif shape == "diamond":
        draw.polygon([(128, 52), (204, 128), (128, 204), (52, 128)], fill=accent)
    elif shape == "hex":
        draw.polygon([(80, 74), (176, 74), (208, 128), (176, 182), (80, 182), (48, 128)], fill=accent)
    else:
        draw.rounded_rectangle((60, 76, 196, 180), radius=34, fill=accent)
    draw.ellipse((100, 100, 156, 156), fill=rgba(inner))
    draw.ellipse((118, 118, 138, 138), fill=rgba("#09131A"))
    save(base, name)


def draw_sigil(name: str, body: str, spikes: int) -> None:
    base = canvas()
    accent = rgba(body)
    paste_add(base, glow_ring(86, accent, 10, 14, 165))
    draw = ImageDraw.Draw(base)
    outer = []
    for i in range(spikes):
        angle = i * 360 / spikes
        from math import cos, radians, sin

        outer.append(
            (
                int(128 + cos(radians(angle)) * 92),
                int(128 + sin(radians(angle)) * 92),
            )
        )
        outer.append(
            (
                int(128 + cos(radians(angle + 360 / spikes / 2)) * 58),
                int(128 + sin(radians(angle + 360 / spikes / 2)) * 58),
            )
        )
    draw.polygon(outer, fill=(accent[0], accent[1], accent[2], 215))
    draw.ellipse((94, 94, 162, 162), fill=rgba("#101822", 220))
    draw.ellipse((108, 108, 148, 148), fill=accent)
    save(base, name)


def draw_trail() -> None:
    base = canvas()
    accent = rgba("#7AF7FF")
    paste_add(base, line_glow([(54, 206), (128, 108), (202, 42)], accent, 34, 16, 170))
    draw = ImageDraw.Draw(base)
    draw.line([(62, 198), (128, 114), (194, 50)], fill=accent, width=18, joint="curve")
    save(base, "fx_trail_thruster")


def draw_dash() -> None:
    base = canvas()
    accent = rgba("#94F9FF")
    paste_add(base, glow_polygon([(40, 146), (164, 74), (232, 102), (108, 174)], accent, 20, 190))
    draw = ImageDraw.Draw(base)
    draw.polygon([(48, 142), (160, 84), (224, 106), (112, 166)], fill=accent)
    draw.line([(72, 156), (198, 92)], fill=rgba("#E4FDFF"), width=10)
    save(base, "fx_dash_streak")


def draw_shield() -> None:
    base = canvas()
    accent = rgba("#8DFFBF")
    paste_add(base, glow_ring(86, accent, 18, 18, 170))
    draw = ImageDraw.Draw(base)
    draw.ellipse((42, 42, 214, 214), outline=rgba("#DAFFF0", 220), width=12)
    draw.arc((54, 54, 202, 202), start=18, end=102, fill=accent, width=18)
    draw.arc((54, 54, 202, 202), start=138, end=222, fill=accent, width=18)
    draw.arc((54, 54, 202, 202), start=258, end=342, fill=accent, width=18)
    save(base, "fx_shield_ring")


def draw_pulse() -> None:
    base = canvas()
    accent = rgba("#B0C2FF")
    paste_add(base, glow_ring(84, accent, 18, 16, 180))
    paste_add(base, glow_ring(52, accent, 12, 10, 130))
    draw = ImageDraw.Draw(base)
    draw.ellipse((74, 74, 182, 182), outline=rgba("#F1F3FF", 220), width=10)
    draw.ellipse((104, 104, 152, 152), fill=rgba("#F9FAFF", 190))
    save(base, "fx_pulse_burst")


def draw_mine() -> None:
    base = canvas()
    accent = rgba("#FFC970")
    paste_add(base, glow_ring(70, accent, 20, 18, 180))
    draw = ImageDraw.Draw(base)
    draw.polygon([(128, 44), (152, 104), (212, 128), (152, 152), (128, 212), (104, 152), (44, 128), (104, 104)], fill=accent)
    draw.ellipse((100, 100, 156, 156), fill=rgba("#24180F", 220))
    draw.ellipse((114, 114, 142, 142), fill=rgba("#FFF1D8", 220))
    save(base, "fx_mine_burst")


def draw_hit() -> None:
    base = canvas()
    accent = rgba("#FF7D9A")
    paste_add(base, line_glow([(58, 128), (198, 128)], accent, 26, 14, 180))
    paste_add(base, line_glow([(128, 58), (128, 198)], accent, 26, 14, 180))
    draw = ImageDraw.Draw(base)
    draw.line([(66, 128), (190, 128)], fill=rgba("#FFF0F4"), width=12)
    draw.line([(128, 66), (128, 190)], fill=rgba("#FFF0F4"), width=12)
    save(base, "fx_hit_spark")


def draw_death() -> None:
    base = canvas()
    hot = rgba("#FFF0BE")
    amber = rgba("#FFB85A")
    coral = rgba("#FF7E76")
    paste_add(base, glow_ring(92, coral, 28, 24, 150))
    paste_add(base, glow_ring(62, amber, 24, 16, 170))
    draw = ImageDraw.Draw(base)
    draw.ellipse((68, 68, 188, 188), fill=(amber[0], amber[1], amber[2], 180))
    draw.ellipse((96, 96, 160, 160), fill=hot)
    save(base, "fx_death_bloom")


def draw_pickup(name: str, body: str, inner: str) -> None:
    base = canvas()
    accent = rgba(body)
    paste_add(base, glow_ring(54, accent, 22, 16, 170))
    draw = ImageDraw.Draw(base)
    draw.ellipse((74, 74, 182, 182), fill=accent)
    draw.ellipse((102, 102, 154, 154), fill=rgba(inner))
    save(base, name)


def draw_badge(name: str, body: str, edge: str) -> None:
    base = canvas()
    paste_add(base, glow_ring(88, rgba(body), 14, 18, 120))
    draw = ImageDraw.Draw(base)
    draw.rounded_rectangle((42, 66, 214, 190), radius=44, fill=rgba(body, 180), outline=rgba(edge, 230), width=10)
    draw.ellipse((98, 92, 158, 152), fill=rgba(edge, 210))
    save(base, name)


def main() -> None:
    draw_ship_striker()
    draw_ship_warden()
    draw_ship_specter()
    draw_enemy_core("fx_enemy_chaser", "#FF6A8B", "#FFF4F7", "circle")
    draw_enemy_core("fx_enemy_shooter", "#7EA0FF", "#EEF1FF", "diamond")
    draw_enemy_core("fx_enemy_tank", "#FFBD5A", "#FFF4DC", "hex")
    draw_enemy_core("fx_enemy_splitter", "#4FFFC6", "#EEFFF8", "diamond")
    draw_enemy_core("fx_enemy_swarm", "#A2FFE7", "#F4FFFB", "circle")
    draw_sigil("fx_sigil_elite", "#AAB5FF", 6)
    draw_sigil("fx_sigil_boss", "#FFCF75", 8)
    draw_trail()
    draw_dash()
    draw_shield()
    draw_pulse()
    draw_mine()
    draw_hit()
    draw_death()
    draw_pickup("fx_pickup_glow_xp", "#9FF2FF", "#F2FEFF")
    draw_pickup("fx_pickup_glow_credit", "#FFCF75", "#FFF7E2")
    draw_badge("fx_badge_modifier", "#1A2332", "#63E7FF")
    draw_badge("fx_badge_reward", "#251A11", "#FFBF66")


if __name__ == "__main__":
    main()
