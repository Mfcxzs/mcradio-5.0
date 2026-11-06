#!/usr/bin/env python3
"""VoidFrequency v4 assets: dark industrial models (BlockBench-compatible JSON), neon textures, blockstates,
loot tables, recipes, tags and lang entries for the eight-block broadcast chain. Stdlib only, idempotent."""
import json, os, random, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "mcradio")
DATA = os.path.join(ROOT, "data")

BG = (18, 18, 18, 255); PANEL = (30, 30, 30, 255); EDGE = (46, 46, 46, 255)
CYAN = (0, 240, 255, 255); PINK = (255, 46, 147, 255); GREEN = (57, 255, 136, 255); AMBER = (255, 176, 0, 255)


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write("\n")


class Img:
    def __init__(self, base, size=16):
        self.s = size
        self.p = [[base] * size for _ in range(size)]

    def set(self, x, y, c):
        if 0 <= x < self.s and 0 <= y < self.s:
            self.p[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def frame(self, c):
        for i in range(self.s):
            for x, y in ((i, 0), (i, self.s - 1), (0, i), (self.s - 1, i)):
                self.set(x, y, c)

    def save(self, name, folder="block"):
        raw = b"".join(b"\x00" + b"".join(bytes(self.p[y][x]) for x in range(self.s)) for y in range(self.s))

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.s, self.s, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
        path = os.path.join(ASSETS, "textures", folder, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, "wb").write(png)


def noisy(base, seed, amount=4):
    rnd = random.Random(seed)
    im = Img(base)
    for y in range(16):
        for x in range(16):
            d = rnd.randint(-amount, amount)
            im.set(x, y, tuple(max(0, min(255, v + d)) for v in base[:3]) + (255,))
    return im


def dim(c, f):
    return (int(c[0] * f), int(c[1] * f), int(c[2] * f), 255)


def make_textures():
    d = noisy((30, 30, 30, 255), 11); d.frame(BG)
    for p in ((1, 1), (14, 1), (1, 14), (14, 14)): d.set(*p, EDGE)
    d.save("void_dark")
    n = noisy((12, 40, 44, 255), 12)
    for y in (2, 7, 12): n.rect(1, y, 14, y, CYAN)
    n.frame(dim(CYAN, 0.5)); n.save("void_neon")
    m = noisy((22, 22, 22, 255), 13)
    for x in range(0, 16, 3): m.rect(x, 0, x, 15, BG)
    m.save("void_vent")
    mast = noisy((60, 62, 66, 255), 14)
    for y in range(0, 16, 4): mast.rect(0, y, 15, y, (35, 36, 40, 255))
    mast.rect(7, 0, 8, 15, (110, 114, 120, 255)); mast.save("void_mast")
    tip = Img(BG); tip.rect(4, 4, 11, 11, PINK); tip.rect(6, 6, 9, 9, (255, 200, 225, 255)); tip.frame(EDGE); tip.save("void_beacon")

    def front(name, accent, pattern, on):
        f = noisy((26, 26, 26, 255), hash(name) % 1000); f.frame(BG)
        col = accent if on else dim(accent, 0.25)
        pattern(f, col, on)
        f.save(name + ("_on" if on else ""))

    def server(f, c, on):
        for i, y in enumerate((2, 5, 8, 11)):
            f.rect(2, y, 13, y + 2, BG); f.rect(3, y + 1, 4, y + 1, c if (on or i == 0) else dim(c, 0.2))
            f.rect(6, y + 1, 12, y + 1, EDGE)

    def computer(f, c, on):
        f.rect(2, 2, 13, 9, BG); f.rect(3, 3, 12, 8, dim(c, 0.35) if on else (10, 10, 10, 255))
        if on:
            for y in (4, 6): f.rect(4, y, 9, y, c)
        for x in range(3, 13, 2): f.rect(x, 11, x, 12, EDGE)
        f.rect(11, 13, 12, 13, c)

    def amp(f, c, on):
        f.rect(2, 2, 13, 4, BG)
        for i in range(8):
            h = 3 + (i * 5 % 7) if on else 1
            f.rect(2 + i * 3 // 2, 12 - h, 2 + i * 3 // 2, 12, c)
        f.rect(2, 13, 13, 13, EDGE)

    def translator(f, c, on):
        for cx in (5, 10):
            f.rect(cx - 2, 3, cx + 2, 7, BG); f.rect(cx - 1, 4, cx + 1, 6, c if on else dim(c, 0.3))
        f.rect(2, 10, 13, 12, BG); f.rect(3, 11, 12, 11, c if on else EDGE)

    for name, col, pat in (("void_server", CYAN, server), ("void_computer", GREEN, computer),
                           ("void_amp", AMBER, amp), ("void_translator", PINK, translator)):
        front(name, col, pat, False); front(name, col, pat, True)

    w = Img((0, 0, 0, 0))
    for i in range(10): w.set(3 + i, 12 - i, (70, 72, 78, 255)); w.set(4 + i, 12 - i, (110, 114, 120, 255))
    w.rect(2, 11, 5, 14, (46, 46, 46, 255)); w.rect(10, 1, 14, 4, (70, 72, 78, 255)); w.rect(12, 2, 13, 3, (0, 0, 0, 0))
    w.rect(3, 12, 4, 13, CYAN); w.save("cable_wrench", "item")


# ---------------------------------------------------------------- models
FACES = ("north", "south", "east", "west", "up", "down")


def box(a, b, tex, north=None):
    faces = {f: {"texture": "#" + tex} for f in FACES}
    if north:
        faces["north"] = {"texture": "#" + north}
    return {"from": list(a), "to": list(b), "faces": faces}


def model(elements, front):
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "#dark", "dark": "mcradio:block/void_dark", "neon": "mcradio:block/void_neon",
                         "vent": "mcradio:block/void_vent", "mast": "mcradio:block/void_mast",
                         "beacon": "mcradio:block/void_beacon", "front": "mcradio:block/" + front},
            "elements": elements}


def build_models():
    base = box((0, 0, 0), (16, 2, 16), "dark")
    strip = lambda y: box((0, y, 0.2), (16, y + 1, 0.4), "neon")
    out = {}
    # music server: rack with a drive-bay front, vented sides, neon rails
    out["music_server"] = lambda f: model([base, box((1, 2, 1), (15, 15, 15), "dark", "front"), box((0, 15, 0), (16, 16, 16), "vent"),
                                           box((0, 2, 0), (1, 15, 1), "neon"), box((15, 2, 0), (16, 15, 1), "neon"),
                                           box((3, 14, 3), (13, 16, 13), "vent")], f)
    # broadcast computer: desk, screen on the front, keyboard slab
    out["broadcast_computer"] = lambda f: model([base, box((1, 2, 3), (15, 9, 15), "dark"), box((2, 9, 6), (14, 15, 14), "dark", "front"),
                                                 box((2, 2.5, 0.5), (14, 4, 3), "neon"), box((1, 9, 5), (15, 10, 6), "neon")], f)
    # amplifier: heat-sink fins on top
    fins = [box((2 + i * 3, 11, 2), (3 + i * 3, 16, 14), "vent") for i in range(5)]
    out["audio_amplifier"] = lambda f: model([base, box((1, 2, 1), (15, 11, 15), "dark", "front"), box((0, 5, 0), (16, 6, 1), "neon")] + fins, f)
    # translator: body, tuning knobs, short mast stub
    out["signal_translator"] = lambda f: model([base, box((1, 2, 1), (15, 12, 15), "dark", "front"), box((7, 12, 7), (9, 16, 9), "mast"),
                                                box((2, 12, 2), (4, 13, 4), "neon"), box((12, 12, 2), (14, 13, 4), "neon")], f)
    # antennas: tier 1 mast, tier 2 adds cross arms, tier 3 adds a dish ring and a beacon
    def antenna(tier):
        els = [box((4, 0, 4), (12, 2, 12), "dark"), box((7, 2, 7), (9, 16, 9), "mast"), box((6, 2, 6), (10, 4, 10), "neon")]
        if tier >= 2:
            els += [box((3, 9, 7.5), (13, 10, 8.5), "mast"), box((7.5, 9, 3), (8.5, 10, 13), "mast")]
        if tier >= 3:
            els += [box((2, 12, 2), (14, 13, 14), "vent"), box((6.5, 13, 6.5), (9.5, 16, 9.5), "beacon")]
        return lambda f: model(els, "void_dark")
    out["antenna_tier1"], out["antenna_tier2"], out["antenna_tier3"] = antenna(1), antenna(2), antenna(3)
    fronts = {"music_server": "void_server", "broadcast_computer": "void_computer", "audio_amplifier": "void_amp",
              "signal_translator": "void_translator"}
    for name, fn in out.items():
        base_front = fronts.get(name)
        if base_front:
            wjson(f"{ASSETS}/models/block/{name}.json", fn(base_front))
            wjson(f"{ASSETS}/models/block/{name}_on.json", fn(base_front + "_on"))
        else:
            wjson(f"{ASSETS}/models/block/{name}.json", fn("void_dark"))
            wjson(f"{ASSETS}/models/block/{name}_on.json", fn("void_dark"))
        wjson(f"{ASSETS}/models/item/{name}.json", {"parent": f"mcradio:block/{name}"})
        v = {}
        for i, facing in enumerate(("north", "east", "south", "west")):
            for active in ("false", "true"):
                e = {"model": f"mcradio:block/{name}" + ("_on" if active == "true" else "")}
                if i: e["y"] = 90 * i
                v[f"facing={facing},active={active}"] = e
        wjson(f"{ASSETS}/blockstates/{name}.json", {"variants": v})
    wjson(f"{ASSETS}/models/item/cable_wrench.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "mcradio:item/cable_wrench"}})
    return list(out)


# ---------------------------------------------------------------- data
def shaped(name, pattern, key, count=1, cat="redstone"):
    wjson(f"{DATA}/mcradio/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
          "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": f"mcradio:{name}", "count": count}})


def make_data(blocks):
    for b in blocks:
        wjson(f"{DATA}/mcradio/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
              "entries": [{"type": "minecraft:item", "name": f"mcradio:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        path = f"{DATA}/minecraft/tags/block/{tag}.json"
        t = json.load(open(path, encoding="utf-8"))
        for b in blocks:
            if f"mcradio:{b}" not in t["values"]: t["values"].append(f"mcradio:{b}")
        wjson(path, t)
    I, B, R, A = "minecraft:iron_ingot", "mcradio:circuit_board", "minecraft:redstone", "mcradio:antenna_rod"
    shaped("music_server", ["IBI", "BJB", "IHI"], {"I": I, "B": B, "J": "minecraft:jukebox", "H": "minecraft:hopper"})
    shaped("broadcast_computer", ["IGI", "BBB", "IRI"], {"I": I, "G": "minecraft:glass_pane", "B": B, "R": R})
    shaped("audio_amplifier", ["IFI", "BRB", "III"], {"I": I, "F": "mcradio:ferrite_coil", "B": B, "R": "minecraft:redstone_block"})
    shaped("signal_translator", ["IAI", "BFB", "III"], {"I": I, "A": A, "B": B, "F": "mcradio:ferrite_coil"})
    shaped("antenna_tier1", ["A", "I", "I"], {"A": A, "I": I})
    shaped("antenna_tier2", [" T ", "ATA", "III"], {"T": "mcradio:antenna_tier1", "A": A, "I": I})
    shaped("antenna_tier3", [" T ", "ASA", "III"], {"T": "mcradio:antenna_tier2", "A": A, "S": "mcradio:satellite_core", "I": I})
    shaped("cable_wrench", ["I I", "IBI", " I "], {"I": I, "B": B}, cat="equipment")


# ---------------------------------------------------------------- lang
EN = {
 "block.mcradio.music_server": "Music Server", "block.mcradio.broadcast_computer": "Broadcast Computer",
 "block.mcradio.audio_amplifier": "Audio Amplifier", "block.mcradio.signal_translator": "Translator",
 "block.mcradio.antenna_tier1": "Antenna (Tier 1)", "block.mcradio.antenna_tier2": "Antenna (Tier 2)",
 "block.mcradio.antenna_tier3": "Antenna (Tier 3)", "item.mcradio.cable_wrench": "Cable Wrench",
 "block.mcradio.signal_station": "Distribution Station", "block.mcradio.signal_transmitter": "Antenna Booster",
 "block.mcradio.signal_repeater": "Repeater", "block.mcradio.signal_horn": "Megaphone",
 "container.mcradio.computer": "Broadcast OS", "container.mcradio.translator": "Translator",
 "message.mcradio.server_library": "Library: %s (%s tracks)", "message.mcradio.duration_pending": "Measuring track length, try again in a moment",
 "message.mcradio.amp_status": "Energy %s / %s FE - %s", "message.mcradio.amp_on": "ACTIVE", "message.mcradio.amp_off": "NO POWER",
 "message.mcradio.antenna_status": "Antenna tier %s, range %s blocks",
 "message.mcradio.wrench.not_chain": "Not a chain block", "message.mcradio.wrench.cleared": "All cables of this block removed",
 "message.mcradio.wrench.selected": "Selected: %s. Click the next block of the chain", "message.mcradio.wrench.cancelled": "Selection cancelled",
 "message.mcradio.wrench.bad_pair": "Cable order is Server - Computer - Amplifier - Station", "message.mcradio.wrench.too_far": "Too far (max %s blocks)",
 "message.mcradio.wrench.linked": "Cable connected", "message.mcradio.wrench.unlinked": "Cable removed",
 "chain.problem.no_station": "No Distribution Station", "chain.problem.no_translator": "No Translator next to the station",
 "chain.problem.no_antenna": "No Antenna next to the Translator", "chain.problem.no_amp": "No Amplifier cabled to the station",
 "chain.problem.no_computer": "No Computer cabled to the Amplifier", "chain.problem.no_server": "No Music Server cabled to the Computer",
 "chain.problem.no_power": "Amplifier has no power", "chain.problem.ok": "OK",
 "gui.mcradio.v4.connecting": "Connecting...", "gui.mcradio.v4.status_ok": "ON AIR  %s  |  Tier %s  |  %s server(s)",
 "gui.mcradio.v4.no_servers": "No servers", "gui.mcradio.v4.add_track": "+ Track to queue", "gui.mcradio.v4.add_voice": "+ As voice insert",
 "gui.mcradio.v4.play": "Play", "gui.mcradio.v4.pause": "Pause", "gui.mcradio.v4.stop": "Stop", "gui.mcradio.v4.next": "Next",
 "gui.mcradio.v4.loop": "Loop", "gui.mcradio.v4.clear": "Clear", "gui.mcradio.v4.announcement": "Announcement",
 "gui.mcradio.v4.announcement_hint": "Announcement text...", "gui.mcradio.v4.queue_text": "Queue text",
 "gui.mcradio.v4.emergency": "EMERGENCY BROADCAST", "gui.mcradio.v4.music_volume": "Music", "gui.mcradio.v4.announcement_volume": "Voice",
 "gui.mcradio.v4.master_volume": "Master", "gui.mcradio.v4.queue": "QUEUE (%s)  inserts: %s",
 "gui.mcradio.v4.ducking": "Music ducked -%s%%", "gui.mcradio.v4.state.stopped": "STOPPED", "gui.mcradio.v4.state.playing": "PLAYING",
 "gui.mcradio.v4.state.paused": "PAUSED", "gui.mcradio.v4.password": "Password", "gui.mcradio.v4.password_hint": "Master password",
 "gui.mcradio.v4.password_set": "OK", "gui.mcradio.v4.encrypted": "ENCRYPTED - enter the password",
 "gui.mcradio.v4.translator_ok": "Chain OK, antenna tier %s", "gui.mcradio.v4.channel_open": "Open channel",
 "gui.mcradio.v4.channel_locked": "ENCRYPTED: wrong or missing password", "gui.mcradio.v4.channel_unlocked": "Secure channel unlocked",
 "gui.mcradio.v4.secure_hint": "Secure channels: 310, 320, 330",
}
RU = {
 "block.mcradio.music_server": "Сервер с музыкой", "block.mcradio.broadcast_computer": "Компьютер вещания",
 "block.mcradio.audio_amplifier": "Усилитель звука", "block.mcradio.signal_translator": "Транслятор",
 "block.mcradio.antenna_tier1": "Антенна (Тир 1)", "block.mcradio.antenna_tier2": "Антенна (Тир 2)",
 "block.mcradio.antenna_tier3": "Антенна (Тир 3)", "item.mcradio.cable_wrench": "Гаечный ключ для кабеля",
 "block.mcradio.signal_station": "Станция раздачи связи", "block.mcradio.signal_transmitter": "Усилитель антенны",
 "block.mcradio.signal_repeater": "Ретранслятор", "block.mcradio.signal_horn": "Мегафон",
 "container.mcradio.computer": "Операционная система вещания", "container.mcradio.translator": "Транслятор",
 "message.mcradio.server_library": "Библиотека: %s (треков: %s)", "message.mcradio.duration_pending": "Считаю длину трека, повторите через секунду",
 "message.mcradio.amp_status": "Энергия %s / %s FE - %s", "message.mcradio.amp_on": "РАБОТАЕТ", "message.mcradio.amp_off": "НЕТ ПИТАНИЯ",
 "message.mcradio.antenna_status": "Антенна тир %s, дальность %s блоков",
 "message.mcradio.wrench.not_chain": "Это не блок цепочки", "message.mcradio.wrench.cleared": "Все кабели блока убраны",
 "message.mcradio.wrench.selected": "Выбрано: %s. Кликните по следующему блоку цепочки", "message.mcradio.wrench.cancelled": "Выбор отменён",
 "message.mcradio.wrench.bad_pair": "Порядок кабелей: Сервер - Компьютер - Усилитель - Станция", "message.mcradio.wrench.too_far": "Слишком далеко (макс. %s блоков)",
 "message.mcradio.wrench.linked": "Кабель подключён", "message.mcradio.wrench.unlinked": "Кабель убран",
 "chain.problem.no_station": "Нет станции раздачи", "chain.problem.no_translator": "Нет транслятора рядом со станцией",
 "chain.problem.no_antenna": "Нет антенны рядом с транслятором", "chain.problem.no_amp": "К станции не подключён усилитель",
 "chain.problem.no_computer": "К усилителю не подключён компьютер", "chain.problem.no_server": "К компьютеру не подключён сервер",
 "chain.problem.no_power": "У усилителя нет питания", "chain.problem.ok": "OK",
 "gui.mcradio.v4.connecting": "Подключение...", "gui.mcradio.v4.status_ok": "В ЭФИРЕ  %s  |  Тир %s  |  серверов: %s",
 "gui.mcradio.v4.no_servers": "Нет серверов", "gui.mcradio.v4.add_track": "+ Трек в очередь", "gui.mcradio.v4.add_voice": "+ Как голосовая врезка",
 "gui.mcradio.v4.play": "Пуск", "gui.mcradio.v4.pause": "Пауза", "gui.mcradio.v4.stop": "Стоп", "gui.mcradio.v4.next": "Далее",
 "gui.mcradio.v4.loop": "Повтор", "gui.mcradio.v4.clear": "Сброс", "gui.mcradio.v4.announcement": "Объявление",
 "gui.mcradio.v4.announcement_hint": "Текст объявления...", "gui.mcradio.v4.queue_text": "В очередь",
 "gui.mcradio.v4.emergency": "ЭКСТРЕННЫЙ ЭФИР", "gui.mcradio.v4.music_volume": "Музыка", "gui.mcradio.v4.announcement_volume": "Голос",
 "gui.mcradio.v4.master_volume": "Общая", "gui.mcradio.v4.queue": "ОЧЕРЕДЬ (%s)  врезок: %s",
 "gui.mcradio.v4.ducking": "Музыка приглушена -%s%%", "gui.mcradio.v4.state.stopped": "СТОП", "gui.mcradio.v4.state.playing": "ИГРАЕТ",
 "gui.mcradio.v4.state.paused": "ПАУЗА", "gui.mcradio.v4.password": "Пароль", "gui.mcradio.v4.password_hint": "Мастер-пароль",
 "gui.mcradio.v4.password_set": "OK", "gui.mcradio.v4.encrypted": "ЗАШИФРОВАНО - введите пароль",
 "gui.mcradio.v4.translator_ok": "Цепочка в порядке, антенна тир %s", "gui.mcradio.v4.channel_open": "Открытый канал",
 "gui.mcradio.v4.channel_locked": "ЗАШИФРОВАНО: нет или неверный пароль", "gui.mcradio.v4.channel_unlocked": "Защищённый канал открыт",
 "gui.mcradio.v4.secure_hint": "Защищённые каналы: 310, 320, 330",
}


def merge_lang(name, updates):
    path = f"{ASSETS}/lang/{name}"
    d = json.load(open(path, encoding="utf-8"))
    d.update(updates)
    wjson(path, d)


if __name__ == "__main__":
    make_textures()
    blocks = build_models()
    make_data(blocks)
    merge_lang("en_us.json", EN)
    merge_lang("ru_ru.json", RU)
    print("v4 assets written:", ", ".join(blocks))
