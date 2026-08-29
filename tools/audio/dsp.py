"""Cisteni velmi tichych nahravek: Wienerovo potlaceni sumu + detekce slysitelnych useku."""
import subprocess, sys, os
import numpy as np
from scipy import signal as sg

FF = subprocess.run([sys.executable, "-c",
     "import imageio_ffmpeg;print(imageio_ffmpeg.get_ffmpeg_exe())"],
     capture_output=True, text=True).stdout.strip()
SR = 48000
NFFT, HOP = 2048, 512


def decode(path, sr=SR):
    r = subprocess.run([FF, "-v", "error", "-i", path, "-map", "0:a:0",
                        "-ac", "1", "-ar", str(sr), "-f", "f32le", "-"],
                       capture_output=True, check=True)
    return np.frombuffer(r.stdout, dtype=np.float32).astype(np.float64)


def denoise(x, over=2.0, floor_db=-16.0, alpha=0.96):
    """Dekonvoluce sumu: minimum-statistics odhad + decision-directed Wiener."""
    f, t, X = sg.stft(x, fs=SR, nperseg=NFFT, noverlap=NFFT - HOP,
                      window="hann", boundary="zeros", padded=True)
    P = np.abs(X) ** 2
    # odhad sumoveho spektra: 15. percentil pres cas (sum je stacionarni, rec ne)
    N = np.percentile(P, 15, axis=1, keepdims=True) * over
    N = np.maximum(N, 1e-18)
    gfloor = 10 ** (floor_db / 20)
    G = np.empty_like(P)
    prev = np.ones(P.shape[0])                       # a-priori SNR z minuleho ramce
    for i in range(P.shape[1]):
        post = P[:, i] / N[:, 0]                      # a-posteriori SNR
        prio = alpha * prev + (1 - alpha) * np.maximum(post - 1, 0)
        g = prio / (1 + prio)
        g = np.maximum(g, gfloor)
        G[:, i] = g
        prev = (g ** 2) * post
    # vyhlazeni zisku pres frekvenci -> potlaci "hudebni" sum
    G = sg.convolve2d(G, np.ones((5, 1)) / 5, mode="same", boundary="symm")
    _, y = sg.istft(X * G, fs=SR, nperseg=NFFT, noverlap=NFFT - HOP, window="hann")
    return y[:len(x)]


def shape(x):
    """Doladeni pasma pro srozumitelnost reci."""
    sos = sg.butter(4, 90, btype="high", fs=SR, output="sos")
    y = sg.sosfilt(sos, x)
    sos = sg.butter(4, 9000, btype="low", fs=SR, output="sos")
    y = sg.sosfilt(sos, y)
    # jemne zvyrazneni srozumitelnosti 1.6-4 kHz (+4 dB)
    b, a = sg.iirpeak(2600 / (SR / 2), Q=0.9)
    y = y + 0.58 * sg.lfilter(b, a, y)
    # potlaceni dunivosti 90-200 Hz (-4 dB)
    b, a = sg.iirpeak(150 / (SR / 2), Q=1.0)
    y = y - 0.37 * sg.lfilter(b, a, y)
    return y


def env_db(x, hop=0.02, win=0.05, lo=300, hi=3800):
    sos = sg.butter(4, [lo, hi], btype="band", fs=SR, output="sos")
    y = sg.sosfilt(sos, x)
    h, w = int(hop * SR), int(win * SR)
    n = max(0, (len(y) - w) // h + 1)
    fr = np.lib.stride_tricks.as_strided(y, (n, w), (y.strides[0] * h, y.strides[0]))
    return 20 * np.log10(np.sqrt((fr ** 2).mean(axis=1)) + 1e-20), hop


def find_segments(raw, clean, margin_raw=4.0, margin_clean=6.0,
                  min_len=0.30, gap=0.70, pad=0.30):
    """Usek se bere jen tam, kde je energie nad sumem v puvodnim
    i ve vycistenem signalu - to odfiltruje kolisani sumu."""
    dr, hop = env_db(raw)
    dc, _ = env_db(clean)
    n = min(len(dr), len(dc))
    dr, dc = dr[:n], dc[:n]
    fr, fc = np.percentile(dr, 20), np.percentile(dc, 25)
    act = sg.medfilt(((dr > fr + margin_raw) & (dc > fc + margin_clean)).astype(float), 7) > 0.5
    segs, i = [], 0
    while i < len(act):
        if act[i]:
            j = i
            while j < len(act) and act[j]:
                j += 1
            segs.append([i * hop, j * hop])
            i = j
        else:
            i += 1
    out = []
    for a, b in segs:
        if out and a - out[-1][1] <= gap:
            out[-1][1] = b
        else:
            out.append([a, b])
    dur = len(clean) / SR
    res = []
    for a, b in out:
        if b - a < min_len:
            continue
        a, b = max(0.0, a - pad), min(dur, b + pad)
        if res and a <= res[-1][1]:
            res[-1][1] = max(res[-1][1], b)
        else:
            res.append([a, b])
    return res, float(fr), float(fc)
