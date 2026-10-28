"""Original background music for ARCROW, synthesized from scratch (no samples, no third-party rights).

Renders three seamless loops to WAV; ffmpeg encodes them to OGG Vorbis afterwards.
"""
import sys
import numpy as np
from scipy.signal import lfilter, butter, fftconvolve
import soundfile as sf

SR = 32000
rng = np.random.default_rng(7)


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def adsr(n, a, d, s, r, sr=SR):
    a, d, r = int(a * sr), int(d * sr), int(r * sr)
    sus = max(n - a - d - r, 0)
    env = np.concatenate([
        np.linspace(0, 1, max(a, 1), endpoint=False),
        np.linspace(1, s, max(d, 1), endpoint=False),
        np.full(sus, s),
        np.linspace(s, 0, max(r, 1)),
    ])
    return env[:n] if len(env) >= n else np.pad(env, (0, n - len(env)))


def saw(f, n, harmonics=18, detune=0.0):
    t = np.arange(n) / SR
    out = np.zeros(n)
    for k in range(1, harmonics + 1):
        if f * k > SR / 2 - 500:
            break
        out += np.sin(2 * np.pi * f * (1 + detune) * k * t) / k
    return out * 0.55


def square(f, n, duty=0.5, harmonics=15):
    t = np.arange(n) / SR
    out = np.zeros(n)
    for k in range(1, harmonics + 1):
        if f * k > SR / 2 - 500:
            break
        out += (2 / (k * np.pi)) * np.sin(np.pi * k * duty) * np.cos(2 * np.pi * f * k * t)
    return out


def tri(f, n):
    t = np.arange(n) / SR
    return 2 * np.abs(2 * ((f * t) % 1) - 1) - 1


def epiano(f, n):
    t = np.arange(n) / SR
    tone = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 6) + \
        0.12 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t * 9)
    return tone * (1 + 0.15 * np.sin(2 * np.pi * 4.5 * t))


def lowpass(x, cutoff, order=2):
    b, a = butter(order, min(cutoff / (SR / 2), 0.99))
    return lfilter(b, a, x)


def highpass(x, cutoff, order=2):
    b, a = butter(order, cutoff / (SR / 2), btype="high")
    return lfilter(b, a, x)


def kick(n=int(0.35 * SR)):
    t = np.arange(n) / SR
    f = 50 + 90 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)


def snare(n=int(0.25 * SR)):
    t = np.arange(n) / SR
    noise = highpass(rng.standard_normal(n), 1200) * np.exp(-t * 18)
    body = np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25)
    return 0.6 * noise + 0.5 * body


def hat(n=int(0.06 * SR)):
    t = np.arange(n) / SR
    return highpass(rng.standard_normal(n), 7000) * np.exp(-t * 70)


def reverb_ir(seconds=2.6, damp=4000):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    left = lowpass(rng.standard_normal(n), damp) * np.exp(-t * 2.6)
    right = lowpass(rng.standard_normal(n), damp) * np.exp(-t * 2.6)
    return left / np.abs(left).max(), right / np.abs(right).max()


class Track:
    def __init__(self, bpm, bars, beats_per_bar=4):
        self.beat = 60 / bpm
        self.bar = self.beat * beats_per_bar
        self.length = int(self.bar * bars * SR)
        tail = int(3 * SR)
        self.dry = np.zeros((2, self.length + tail))
        self.wet_send = np.zeros((2, self.length + tail))
        self.duck = np.ones(self.length + tail)

    def add(self, sig, at_seconds, gain=1.0, pan=0.0, send=0.0):
        i = int(at_seconds * SR)
        n = min(len(sig), self.dry.shape[1] - i)
        if n <= 0:
            return
        l, r = gain * np.sqrt((1 - pan) / 2), gain * np.sqrt((1 + pan) / 2)
        self.dry[0, i:i + n] += sig[:n] * l
        self.dry[1, i:i + n] += sig[:n] * r
        self.wet_send[0, i:i + n] += sig[:n] * l * send
        self.wet_send[1, i:i + n] += sig[:n] * r * send

    def sidechain(self, at_seconds, depth=0.35, release=0.22):
        i = int(at_seconds * SR)
        n = int(release * SR)
        n = min(n, len(self.duck) - i)
        self.duck[i:i + n] = np.minimum(self.duck[i:i + n], 1 - depth * (1 - np.linspace(0, 1, n)))

    def render(self, ducked, reverb=(2.6, 4000), wet=0.28):
        irl, irr = reverb_ir(*reverb)
        wet_l = fftconvolve(self.wet_send[0], irl)[: self.dry.shape[1]]
        wet_r = fftconvolve(self.wet_send[1], irr)[: self.dry.shape[1]]
        mix = self.dry + wet * np.stack([wet_l, wet_r]) / 12
        mix += ducked * self.duck  # pads etc. that pump with the kick
        # Seamless loop: fold the tail back onto the start.
        out = mix[:, : self.length].copy()
        tail = mix[:, self.length:]
        out[:, : tail.shape[1]] += tail
        out = np.tanh(out * 1.1)  # gentle saturation
        peak = np.abs(out).max()
        return (out / peak * 0.7).T


def chord_tones(root, kind):
    shapes = {"m": [0, 3, 7], "M": [0, 4, 7], "M7": [0, 4, 7, 11], "m7": [0, 3, 7, 10], "6": [0, 4, 7, 9], "add9": [0, 4, 7, 14]}
    return [root + i for i in shapes[kind]]


def neon_drift():
    """Synthwave, 96 BPM, A minor: Am – F – C – G."""
    t = Track(96, 32)
    prog = [(57, "m"), (53, "M"), (48, "M"), (55, "M")]
    pads = np.zeros((2, t.dry.shape[1]))
    melody = [76, 74, 72, 74, 76, 79, 76, 74, 72, 71, 72, 74, 76, 0, 74, 72]
    for bar in range(32):
        root, kind = prog[(bar // 2) % 4]
        start = bar * t.bar
        tones = chord_tones(root, kind)
        if bar % 2 == 0:  # pad, two bars per chord
            n = int(t.bar * 2 * SR)
            for k, m in enumerate(tones):
                for side, det in ((0, -0.004), (1, 0.004)):
                    v = lowpass(saw(hz(m), n, 14, det), 1800) * adsr(n, 0.6, 0.5, 0.8, 0.8) * 0.16
                    i = int(start * SR)
                    pads[side, i:i + n] += v[: pads.shape[1] - i]
        for b in range(8):  # bass 8ths
            n = int(t.beat / 2 * SR * 0.9)
            v = lowpass(saw(hz(root - 12), n, 10), 700) * adsr(n, 0.005, 0.08, 0.6, 0.05)
            t.add(v, start + b * t.beat / 2, 0.42)
        for s in range(16):  # arp 16ths
            m = tones[s % len(tones)] + 24
            n = int(0.22 * SR)
            v = lowpass(square(hz(m), n, 0.3), 3500) * adsr(n, 0.002, 0.12, 0.0, 0.05)
            t.add(v, start + s * t.beat / 4, 0.09, pan=0.5 if s % 2 else -0.5, send=0.7)
        if bar >= 4:
            for beat in range(4):
                if beat in (0, 2):
                    t.add(kick(), start + beat * t.beat, 0.55)
                    t.sidechain(start + beat * t.beat)
                if beat in (1, 3) and bar >= 8:
                    t.add(snare(), start + beat * t.beat, 0.22, send=0.6)
                for h in range(2):
                    t.add(hat(), start + (beat + h / 2) * t.beat, 0.07 if h else 0.04, pan=0.3)
        if 16 <= bar < 32:  # melody in the second half
            for step in range(4):
                m = melody[((bar - 16) * 4 + step) % len(melody)]
                if m:
                    n = int(t.beat * SR * 0.95)
                    v = lowpass(saw(hz(m), n, 12, 0.002), 2600) * adsr(n, 0.04, 0.2, 0.6, 0.25)
                    t.add(v, start + step * t.beat, 0.13, send=0.8)
    return t.render(pads)


def pixel_garden():
    """Mellow chiptune, 88 BPM, C major: Cmaj7 – Am7 – Fmaj7 – G6."""
    t = Track(88, 32)
    prog = [(48, "M7"), (45, "m7"), (41, "M7"), (43, "6")]
    lead = [79, 76, 72, 76, 77, 76, 74, 72, 74, 76, 79, 81, 79, 0, 76, 0]
    soft = np.zeros((2, t.dry.shape[1]))
    for bar in range(32):
        root, kind = prog[bar % 4]
        start = bar * t.bar
        tones = chord_tones(root, kind)
        for b in range(4):  # triangle bass
            n = int(t.beat * SR * 0.85)
            v = tri(hz(root - 12 + (7 if b == 2 else 0)), n) * adsr(n, 0.005, 0.1, 0.7, 0.05)
            t.add(v, start + b * t.beat, 0.35)
        for s in range(8):  # pulse arpeggio
            m = tones[(s * 2) % len(tones)] + 12 + (12 if s % 4 == 3 else 0)
            n = int(t.beat / 2 * SR * 0.6)
            v = square(hz(m), n, 0.125) * adsr(n, 0.002, 0.06, 0.3, 0.04)
            t.add(lowpass(v, 4000), start + s * t.beat / 2, 0.08, pan=-0.35, send=0.5)
        n = int(t.bar * SR)  # soft chord bed
        for m in tones:
            v = lowpass(square(hz(m + 12), n, 0.5, 7), 1400) * adsr(n, 0.3, 0.3, 0.5, 0.4) * 0.05
            i = int(start * SR)
            soft[:, i:i + n] += v[: soft.shape[1] - i]
        if bar >= 8:  # lead
            for step in range(4):
                m = lead[((bar - 8) * 4 + step) % len(lead)]
                if m:
                    n = int(t.beat * SR * 0.8)
                    v = square(hz(m), n, 0.25) * adsr(n, 0.004, 0.15, 0.55, 0.1)
                    vib = 1 + 0.003 * np.sin(2 * np.pi * 5 * np.arange(n) / SR)
                    t.add(lowpass(v * vib, 5000), start + step * t.beat, 0.1, pan=0.25, send=0.6)
        if bar >= 4:  # light noise drums
            for beat in range(4):
                t.add(kick(int(0.18 * SR)) * 0.8, start + beat * t.beat, 0.35 if beat in (0, 2) else 0)
                if beat in (1, 3):
                    t.add(lowpass(snare(int(0.12 * SR)), 5000), start + beat * t.beat, 0.12)
                t.add(hat(), start + (beat + 0.5) * t.beat, 0.04)
    return t.render(soft, reverb=(1.8, 5000), wet=0.22)


def after_hours():
    """Ambient lo-fi, 72 BPM, D major: Dmaj7 – Bm7 – Gmaj7 – A6."""
    t = Track(72, 24)
    prog = [(50, "M7"), (47, "m7"), (43, "M7"), (45, "6")]
    bed = np.zeros((2, t.dry.shape[1]))
    for bar in range(24):
        root, kind = prog[bar % 4]
        start = bar * t.bar
        tones = chord_tones(root + 12, kind)
        for k, m in enumerate(tones):  # electric piano, slightly strummed
            n = int(t.bar * SR)
            v = epiano(hz(m), n) * adsr(n, 0.01, 1.2, 0.35, 0.6)
            t.add(lowpass(v, 3000), start + k * 0.03, 0.11, pan=(k - 1.5) * 0.25, send=0.8)
        if bar % 2 == 1:  # little melodic answer
            m = tones[-1] + 12
            n = int(t.beat * 2 * SR)
            t.add(epiano(hz(m), n) * adsr(n, 0.01, 0.8, 0.2, 0.5), start + 2 * t.beat, 0.07, pan=0.4, send=1.0)
        n = int(t.bar * SR)  # bass
        t.add(lowpass(np.sin(2 * np.pi * hz(root - 12) * np.arange(n) / SR), 400) * adsr(n, 0.02, 0.6, 0.5, 0.4), start, 0.45)
        n = int(t.bar * SR)  # airy pad
        for m in tones[:3]:
            v = lowpass(saw(hz(m), n, 8, 0.003), 900) * adsr(n, 1.2, 0.5, 0.7, 1.0) * 0.05
            i = int(start * SR)
            bed[:, i:i + n] += v[: bed.shape[1] - i]
        if bar >= 4:  # brushed beat
            for beat in range(4):
                if beat in (0, 2):
                    t.add(lowpass(kick(), 2500), start + beat * t.beat + (0.0 if beat == 0 else 0.04), 0.45)
                if beat in (1, 3):
                    t.add(lowpass(snare(), 3500), start + beat * t.beat, 0.13, send=0.5)
                for h in range(2):
                    t.add(lowpass(hat(), 9000), start + (beat + h / 2 + (0.08 if h else 0)) * t.beat, 0.035)
    # vinyl crackle
    crackle = np.zeros(t.dry.shape[1])
    pops = rng.integers(0, t.length, size=int(t.length / SR * 6))
    crackle[pops] = rng.uniform(-1, 1, size=len(pops))
    crackle = lowpass(crackle, 6000) + lowpass(rng.standard_normal(len(crackle)), 3000) * 0.004
    bed += np.stack([crackle, crackle]) * 0.25
    return t.render(bed, reverb=(3.2, 3000), wet=0.35)


def pulse(f, n, duty):
    """Naive NES-style pulse wave, softened just enough above Nyquist to avoid harsh aliasing."""
    t = np.arange(n) / SR
    return lowpass(np.where((f * t) % 1 < duty, 1.0, -1.0), 9000)


def chip_noise(n, rate=12000, decay=40.0, seed=0):
    """Noise channel: random levels held at [rate] Hz (a coarser rate sounds lower and grittier)."""
    r = np.random.default_rng(seed)
    hold = max(int(SR / rate), 1)
    levels = r.choice([-1.0, 1.0], size=n // hold + 1)
    t = np.arange(n) / SR
    return np.repeat(levels, hold)[:n] * np.exp(-t * decay)


def after_hours_8bit():
    """"After Hours" as an arcade chiptune: same D major changes (Dmaj7 – Bm7 – Gmaj7 – A6) and its
    high answer phrase, at 112 BPM on two pulse channels, a triangle bass and noise drums."""
    t = Track(112, 32)
    prog = [(50, "M7"), (47, "m7"), (43, "M7"), (45, "6")]
    eighth = t.beat / 2
    sixteenth = t.beat / 4
    # Lead phrases over each chord, in eighths (0 = rest). Built from the chord tones an octave up,
    # ending each bar on the original's "answer" note (the chord's top tone).
    phrase_a = {
        0: [74, 0, 78, 81, 0, 78, 81, 85],
        1: [83, 0, 81, 78, 0, 74, 78, 81],
        2: [79, 0, 78, 74, 0, 71, 74, 78],
        3: [76, 0, 73, 69, 73, 76, 78, 0],
    }
    phrase_b = {
        0: [86, 85, 81, 78, 81, 0, 85, 0],
        1: [83, 81, 78, 74, 78, 0, 81, 0],
        2: [83, 79, 78, 74, 71, 74, 78, 0],
        3: [81, 78, 76, 73, 76, 78, 81, 81],
    }
    for bar in range(32):
        root, kind = prog[bar % 4]
        start = bar * t.bar
        tones = chord_tones(root + 12, kind)
        # Triangle bass: octave-bouncing eighths.
        for b in range(8):
            m = root - 12 + (12 if b % 2 else 0)
            n = int(eighth * SR * 0.9)
            t.add(tri(hz(m), n) * adsr(n, 0.002, 0.05, 0.8, 0.02), start + b * eighth, 0.42)
        # Pulse 2: fast 12.5% arpeggio up and down the chord.
        if bar >= 2:
            order = tones + tones[-2:0:-1]
            for s in range(16):
                m = order[s % len(order)] + 12
                n = int(sixteenth * SR * 0.7)
                t.add(pulse(hz(m), n, 0.125) * adsr(n, 0.001, 0.04, 0.4, 0.01), start + s * sixteenth, 0.07, pan=-0.3)
        # Pulse 1: the lead (A, A, B, A sections), with a quiet echo three sixteenths later.
        if bar >= 4:
            section = (bar - 4) // 8
            phrase = (phrase_b if section == 1 else phrase_a)[bar % 4]
            for s, m in enumerate(phrase):
                if not m:
                    continue
                held = 2 if s + 1 < len(phrase) and phrase[s + 1] == 0 else 1
                n = int(eighth * held * SR * 0.92)
                v = pulse(hz(m), n, 0.25) * adsr(n, 0.002, 0.08, 0.7, 0.03)
                vib = 1 + 0.004 * np.sin(2 * np.pi * 6 * np.arange(n) / SR) * np.clip(np.arange(n) / (0.15 * SR), 0, 1)
                at = start + s * eighth
                t.add(v * vib, at, 0.12, pan=0.15)
                t.add(v * vib, at + 3 * sixteenth, 0.04, pan=-0.4)
        # Noise drums: kick blip on 1 and 3, snare on 2 and 4, closed hats on the off-beats.
        if bar >= 4:
            for beat in range(4):
                at = start + beat * t.beat
                if beat in (0, 2):
                    n = int(0.12 * SR)
                    tt = np.arange(n) / SR
                    f = 160 * np.exp(-tt * 28) + 45
                    blip = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * np.exp(-tt * 22)
                    t.add(lowpass(blip, 3000), at, 0.32)
                if beat in (1, 3):
                    t.add(chip_noise(int(0.16 * SR), 9000, 20, seed=bar * 4 + beat), at, 0.16)
                t.add(chip_noise(int(0.04 * SR), 24000, 90, seed=99), at + eighth, 0.05)
                if bar % 4 == 3 and beat == 3:  # fill
                    for k in range(4):
                        t.add(chip_noise(int(0.05 * SR), 7000, 50, seed=k), at + k * sixteenth, 0.1)
    # Chiptune stays dry: just a short room so it doesn't sound boxed in.
    return t.render(np.zeros_like(t.dry), reverb=(0.6, 6000), wet=0.08)


if __name__ == "__main__":
    out = sys.argv[1]
    only = set(sys.argv[2:])
    tracks = [("neon_drift", neon_drift), ("pixel_garden", pixel_garden), ("after_hours", after_hours),
              ("after_hours_8bit", after_hours_8bit)]
    for name, fn in [(n, f) for n, f in tracks if not only or n in only]:
        audio = fn()
        sf.write(f"{out}/{name}.wav", audio, SR)
        print(name, f"{len(audio) / SR:.1f}s")
