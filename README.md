# VoidFrequency (mod id: mcradio) - Forge 1.21.1, Java 21

Industrial radio mod: streams, repeaters, cables, jammers, number stations.

## Physics
- Signal quality at a receiver = distance falloff (fades near the edge of a radius) x depth x shielding x (1 - jam).
- Depth: full above Y=60, smooth fade below, total radio silence (white noise) at Y<=0.
- Obsidian, crying obsidian, iron blocks and raw iron blocks block signals on the line to the receiver
  (and above a receiver for the sky signal of number stations).
- Antenna Cable networks link repeaters: a repeater whose input frequency equals the output frequency of an
  active repeater on the same cable network re-emits it with no depth loss, at any depth.
- Quality < 100 %: the client attenuates audio and mixes static/crackle; 0 %: pure white noise.

## Blocks
Distributor (emitter, MP3 playlist / live mic), Computer (remote terminal for a distributor), Transmitter (+32
blocks radius per connected antenna), Repeater, Radio Receiver (32 blocks), Industrial Horn (64 blocks,
distorted), Wall Speaker (16 blocks), Antenna Cable, Signal Jammer (radius 8-96, GUI switch or redstone).

## Items
Walkie-Talkie (300 blocks, hold V), Field Radio (800 blocks, heavy), Pocket Player (needle shows quality),
Radio Scanner (overhears relayed talk), Radio Decoder (number station ciphers), Radio Tablet (map with coverage,
pulsing jammer fields), Circuit Board, Ferrite Coil, Antenna Rod, Satellite Core.

## Number stations
Frequencies 313, 666, 1947, 2077 (any receiver or pocket player). They send a Morse cipher (10 digits). Type it
into the Decoder, or let it capture from a receiver within 8 blocks; the Decoder reveals the site coordinates
(bunker or crashed satellite, generated once, with loot).

## Build
JDK 21, Gradle 8.8+ (`gradle wrapper`, then `./gradlew build`) or the GitHub Actions workflow in `.github`.
Put `.mp3` files into `config/mc_radio/tracks/` on the machine hosting the world.
`tools/generate_assets.py` regenerates textures, models, lang, recipes; `tools/merge_single_file.py` merges all
classes into `single_file/McRadio.java`.


## v4: broadcast chain (new)
Eight steps: Music Server -> Broadcast Computer -> Audio Amplifier -> Distribution Station (joined by invisible
cables) | Translator | Antenna (tier 1/2/3 = 50/100/200 blocks) -> Repeaters -> Radios / Megaphones.
- Cables: Cable Wrench. Click block A, then block B (order Server-Computer-Amplifier-Station, max 24 blocks);
  sneak-click removes all cables of a block. Cables are drawn as neon lines only while the wrench is in a hand.
- Translator and Antenna are placed face-adjacent: Station | Translator | Antenna. A station with a Translator
  next to it belongs to the v4 chain; without one it behaves like v3.
- Amplifier needs FE (20000 FE buffer, 8 FE/tick) or a spinning Create block (>= 16 RPM) touching it.
- Computer: per-second player (seek, pause, stop), queue, voice/text inserts, Emergency Broadcast (music is
  ducked to 30% and keeps running under the announcement), music/voice/master mixer, redstone = alarm.
- Music Server: right-click cycles its library (root of config/mc_radio/tracks or one sub-folder).
- Secure channels 310/320/330: the Translator and every Receiver need the master password, otherwise white noise.
- Thunderstorm: all antenna/repeater radii -30%, receivers never get cleaner than 88% (crackle).
- Regenerate v4 assets: `python3 tools/generate_assets_v4.py`. Logic tests: see tools/CoreSelftest.java.
