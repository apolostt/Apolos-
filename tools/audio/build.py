#!/usr/bin/env python3
"""Vytahne slysitelne useky ze vsech nahravek, vycisti, zesili a slozi do jednoho MP3."""
import os, re, sys, glob, json, subprocess
import numpy as np
from scipy import signal as sg
from scipy.io import wavfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dsp
from dsp import SR, FF

SRC = sys.argv[1] if len(sys.argv) > 1 else "/root/.claude/uploads"
OUT = sys.argv[2] if len(sys.argv) > 2 else "out"
os.makedirs(OUT, exist_ok=True)

MARGIN = 6.0          # dB nad sumovym dnem = "je neco slyset"
TARGET_DB = -20.0     # cilova hlasitost useku v pasmu reci
MAX_GAIN = 46.0       # strop zesileni, aby se nezvedl jen sum
FADE = 0.04           # s
GAP = 0.35            # ticho mezi useky


def part_no(p):
    m = re.search(r"part(\d+)of", os.path.basename(p))
    return int(m.group(1)) if m else 0


def bandrms_db(x, lo=300, hi=3400):
    sos = sg.butter(4, [lo, hi], btype="band", fs=SR, output="sos")
    return 20 * np.log10(np.sqrt((sg.sosfilt(sos, x) ** 2).mean()) + 1e-20)


def fade(x, n):
    n = min(n, len(x) // 2)
    if n <= 0:
        return x
    r = np.linspace(0, 1, n)
    x[:n] *= r
    x[-n:] *= r[::-1]
    return x


def compress(x, thr_db=-24.0, ratio=3.0, atk=0.010, rel=0.180):
    """Mekka komprese - zvedne tise pasaze, srovna hlasite."""
    env = np.abs(sg.hilbert(x)) if len(x) < 2_000_000 else np.abs(x)
    a_a, a_r = np.exp(-1 / (atk * SR)), np.exp(-1 / (rel * SR))
    e = np.empty_like(env); prev = 0.0
    for i, v in enumerate(env):
        c = a_a if v > prev else a_r
        prev = c * prev + (1 - c) * v
        e[i] = prev
    edb = 20 * np.log10(e + 1e-12)
    over = np.maximum(edb - thr_db, 0)
    gain_db = -over * (1 - 1 / ratio)
    return x * 10 ** (gain_db / 20)


files = sorted(glob.glob(os.path.join(SRC, "*.m4a")), key=part_no)
pieces, report = [], []
for p in files:
    raw = dsp.decode(p)
    clean = dsp.shape(dsp.denoise(raw, over=4.0, floor_db=-24.0))
    segs, floor_raw, floor_cl = dsp.find_segments(raw, clean)
    kept = 0.0
    for a, b in segs:
        seg = clean[int(a * SR):int(b * SR)].copy()
        g = min(MAX_GAIN, TARGET_DB - bandrms_db(seg))
        seg *= 10 ** (g / 20)
        seg = compress(seg)
        seg = fade(seg, int(FADE * SR))
        pieces.append(seg)
        pieces.append(np.zeros(int(GAP * SR)))
        kept += b - a
    report.append(dict(file=os.path.basename(p), part=part_no(p),
                       floor_raw_db=round(floor_raw, 1), floor_clean_db=round(floor_cl, 1),
                       n_segs=len(segs), kept_s=round(kept, 2),
                       segs=[[round(a, 2), round(b, 2)] for a, b in segs]))
    print(f"part{part_no(p):03d}: {len(segs):2d} useku, {kept:6.1f}s slyset "
          f"(sumove dno {floor_raw:.1f} dB)", flush=True)

y = np.concatenate(pieces) if pieces else np.zeros(SR)
peak = np.abs(y).max()
if peak > 0:
    y = y / peak * 0.89
wav = os.path.join(OUT, "sestrih.wav")
wavfile.write(wav, SR, y.astype(np.float32))
json.dump(report, open(os.path.join(OUT, "usek_report.json"), "w"), indent=1)
print(f"\nsestrih: {len(y)/SR:.1f}s z {len(files)*120:.0f}s")

mp3 = os.path.join(OUT, "sestrih_cisteno.mp3")
chain = ("speechnorm=e=6.25:r=0.00001:l=1,"
         "loudnorm=I=-16:TP=-1.5:LRA=11,"
         "alimiter=limit=0.95:level=disabled")
subprocess.run([FF, "-y", "-v", "error", "-i", wav, "-af", chain,
                "-ar", "48000", "-ac", "1", "-c:a", "libmp3lame",
                "-b:a", "256k", "-q:a", "0", mp3], check=True)
print("hotovo:", mp3, os.path.getsize(mp3), "B")
