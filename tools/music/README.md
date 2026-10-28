# Everest Frontend background music

`app/src/main/res/raw/music_*.ogg` are original tracks synthesized by `compose.py` (no samples, no
third-party rights). To regenerate:

```
pip install numpy scipy soundfile
python3 compose.py out/
for n in neon_drift pixel_garden after_hours; do
  ffmpeg -y -i out/$n.wav -c:a libvorbis -q:a 3 ../../app/src/main/res/raw/music_$n.ogg
done
```

- **Neon Drift** — synthwave, 96 BPM, A minor
- **Pixel Garden** — mellow chiptune, 88 BPM, C major
- **After Hours** — ambient lo-fi, 72 BPM, D major
