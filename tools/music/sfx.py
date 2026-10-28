"""8-bit menu sounds for Everest, synthesized from scratch (pulse and triangle waves, no samples).

python3 sfx.py <res/raw folder>  — writes ui_*.wav (16-bit mono, 32 kHz).
"""
import sys
import numpy as np
import soundfile as sf

SR = 32000


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def pulse(f, seconds, duty=0.25, decay=0.0):
    t = np.arange(int(seconds * SR)) / SR
    wave = np.where((f * t) % 1 < duty, 1.0, -1.0)
    env = np.exp(-t * decay) if decay else np.ones_like(t)
    fade = np.minimum(1, (len(t) - np.arange(len(t))) / (0.004 * SR))  # no click at the end
    return wave * env * fade


def tri(f, seconds, decay=0.0):
    t = np.arange(int(seconds * SR)) / SR
    wave = 2 * np.abs(2 * ((f * t) % 1) - 1) - 1
    return wave * (np.exp(-t * decay) if decay else 1)


def seq(*parts):
    return np.concatenate(parts)


def write(folder, name, x, peak=0.32):
    x = x / np.abs(x).max() * peak
    sf.write(f"{folder}/{name}.wav", x.astype(np.float32), SR, subtype="PCM_16")


def sine(f, seconds, decay=0.0):
    t = np.arange(int(seconds * SR)) / SR
    return np.sin(2 * np.pi * f * t) * (np.exp(-t * decay) if decay else 1)


def at(track, start, x):
    """Mixes x into track at start seconds."""
    i = int(start * SR)
    track[i:i + len(x)] += x[:max(0, len(track) - i)]


def ps2_boot():
    """In the spirit of the PS2 start-up: a rushing swell, a burst of light, a hush, deep towers, a final chord."""
    total = np.zeros(int(6.2 * SR))
    rng = np.random.default_rng(2000)
    # Rush: noise that swells and brightens for 0.65 s.
    n = int(0.65 * SR)
    noise = rng.standard_normal(n)
    env = np.linspace(0, 1, n) ** 3
    smooth = np.convolve(noise, np.ones(24) / 24, mode="same")
    at(total, 0.0, (smooth * 0.6 + noise * 0.4 * env) * env * 0.5)
    # Burst: low boom plus a bright bell.
    at(total, 0.62, sine(hz(33), 2.2, 1.6) + sine(hz(45), 2.2, 2.0) * 0.6)
    at(total, 0.62, sum(sine(hz(m), 1.6, 2.5) for m in (81, 88, 93)) * 0.18)
    # Hush under the title card.
    at(total, 1.1, sum(sine(hz(m), 1.6, 1.2) for m in (57, 64, 69)) * 0.12)
    # Towers: a deep, slowly opening drone with sparkles.
    drone = sum(sine(hz(m), 3.6, 0.35) for m in (38, 45, 50, 57)) * 0.22
    drone *= np.minimum(1, np.arange(len(drone)) / (0.8 * SR))
    at(total, 2.4, drone)
    for i, m in enumerate((86, 93, 98, 91, 100, 95)):
        at(total, 2.8 + i * 0.32 + rng.uniform(0, 0.1), sine(hz(m), 0.7, 6) * 0.1)
    # Logo: a wide, warm chord.
    at(total, 4.3, sum(sine(hz(m), 1.9, 1.4) for m in (50, 57, 62, 66, 69, 74)) * 0.16)
    fade = np.minimum(1, (len(total) - np.arange(len(total))) / (0.3 * SR))
    return total * fade


if __name__ == "__main__":
    out = sys.argv[1]
    write(out, "ui_move", pulse(hz(88), 0.028, 0.125, 60), 0.22)
    write(out, "ui_select", seq(pulse(hz(84), 0.035, 0.25), pulse(hz(91), 0.06, 0.25, 30)))
    write(out, "ui_back", seq(pulse(hz(84), 0.035, 0.25), pulse(hz(77), 0.06, 0.25, 30)))
    write(out, "ui_tab", seq(*[pulse(hz(m), 0.018, 0.5) for m in (79, 83, 86)]), 0.24)
    # Insert coin: the classic two-note pickup.
    write(out, "ui_launch", seq(pulse(hz(83), 0.075, 0.25), pulse(hz(88), 0.42, 0.25, 7)), 0.34)
    write(out, "ui_error", seq(pulse(hz(45), 0.11, 0.5), np.zeros(int(0.03 * SR)), pulse(hz(41), 0.16, 0.5, 8)), 0.3)
    # Achievement unlocked: fast arpeggio up to a held chord tone.
    write(out, "ui_achievement", seq(*[pulse(hz(m), 0.06, 0.25) for m in (72, 76, 79, 84, 88)], pulse(hz(91), 0.5, 0.25, 4)), 0.32)
    # Secret found: rising scale with a triangle bass under it.
    scale = seq(*[pulse(hz(m), 0.075, 0.125) for m in (72, 74, 76, 77, 79, 81, 83, 84)], pulse(hz(96), 0.45, 0.25, 5))
    bass = tri(hz(48), len(scale) / SR, 1.5)
    write(out, "ui_secret", scale + 0.6 * bass, 0.32)
    # Boot tributes (original sounds in the spirit of each console's start-up).
    write(out, "ui_boot_gb", seq(np.zeros(int(0.05 * SR)), pulse(hz(84), 0.07, 0.5), pulse(hz(96), 0.9, 0.5, 4)), 0.3)
    pad = sum(tri(hz(m), 1.6, 1.2) for m in (48, 55, 60, 64))
    shimmer = seq(np.zeros(int(0.5 * SR)), sum(pulse(hz(m), 1.1, 0.125, 3) for m in (79, 84, 88)))
    write(out, "ui_boot_ps", pad + 0.25 * np.pad(shimmer, (0, max(0, len(pad) - len(shimmer))))[:len(pad)], 0.3)
    write(out, "ui_boot_gc", seq(*[pulse(hz(m), 0.09, 0.25) for m in (72, 79, 76, 84)], pulse(hz(88), 0.7, 0.25, 4)), 0.3)
    write(out, "ui_boot_dc", seq(*[tri(hz(m), 0.12) for m in (64, 69, 71, 76)], tri(hz(81), 1.0, 2.5)) + 0, 0.32)
    # Here comes a new challenger: alarm-like two-tone stabs.
    stab = seq(pulse(hz(81), 0.09, 0.5), pulse(hz(76), 0.09, 0.5))
    write(out, "ui_challenger", seq(stab, stab, stab, pulse(hz(88), 0.5, 0.5, 4)), 0.32)
    # 1UP: quick rising chime.
    write(out, "ui_1up", seq(*[pulse(hz(m), 0.06, 0.25) for m in (79, 83, 86)], pulse(hz(91), 0.3, 0.25, 6)), 0.3)
    # More tributes, one per console (original jingles, nothing sampled).
    rest = lambda sec: np.zeros(int(sec * SR))
    write(out, "ui_boot_gba", seq(rest(0.1), *[pulse(hz(m), 0.06, 0.125) for m in (84, 88, 91)], pulse(hz(96), 0.8, 0.125, 3.5)), 0.3)
    write(out, "ui_boot_gbc", seq(*[pulse(hz(m), 0.05, 0.25) for m in (79, 84, 88, 91)], pulse(hz(96), 0.7, 0.5, 4)), 0.3)
    chord = sum(pulse(hz(m), 1.0, 0.25, 2.5) for m in (60, 64, 67, 72))
    write(out, "ui_boot_md", seq(*[pulse(hz(m), 0.11, 0.5) for m in (67, 72)], chord), 0.3)
    write(out, "ui_boot_3ds", seq(*[tri(hz(m), 0.09) for m in (76, 81, 85)], tri(hz(88), 0.9, 3) + 0.4 * tri(hz(93), 0.9, 4)), 0.32)
    write(out, "ui_boot_n64", seq(*[pulse(hz(m), 0.08, 0.25) for m in (60, 67, 72, 76)], sum(tri(hz(m), 0.9, 2.5) for m in (60, 67, 76))), 0.3)
    write(out, "ui_boot_nds", seq(rest(0.05), tri(hz(88), 0.12), tri(hz(93), 0.9, 3.5)), 0.32)
    write(out, "ui_boot_nes", seq(*[pulse(hz(m), 0.07, 0.5) for m in (72, 76, 79, 84, 79, 84)], pulse(hz(88), 0.4, 0.5, 5)), 0.28)
    write(out, "ui_boot_switch", seq(pulse(hz(60), 0.03, 0.5, 40), rest(0.08), pulse(hz(67), 0.03, 0.5, 40), rest(0.25), tri(hz(84), 0.5, 5)), 0.32)
    write(out, "ui_boot_ps2", ps2_boot(), 0.45)
    write(out, "ui_boot_psp", sum(tri(hz(m), 1.4, 2) for m in (64, 71, 76, 83)) * 0.6 + seq(rest(0.3), tri(hz(88), 1.1, 3)), 0.3)
    write(out, "ui_boot_snes", seq(*[pulse(hz(m), 0.06, 0.25) for m in (76, 79, 83, 88)], sum(pulse(hz(m), 0.8, 0.25, 3) for m in (88, 92, 95))), 0.3)
    write(out, "ui_boot_wii", seq(*[tri(hz(m), 0.14) for m in (79, 83, 86)], tri(hz(91), 0.9, 2.5)), 0.34)
    print("ok")
