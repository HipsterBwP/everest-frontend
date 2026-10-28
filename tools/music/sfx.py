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
    print("ok")
