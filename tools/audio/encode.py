"""Dvoupruchodova normalizace hlasitosti a export do MP3."""
import json, os, re, subprocess, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dsp import FF

wav, mp3 = sys.argv[1], sys.argv[2]
PRE = "speechnorm=e=6.25:r=0.00001:l=1"
LN = "loudnorm=I=-16:TP=-1.5:LRA=11"

# 1. pruchod - zmereni
r = subprocess.run([FF, "-v", "info", "-i", wav, "-af", f"{PRE},{LN}:print_format=json",
                    "-f", "null", "-"], capture_output=True, text=True)
m = re.search(r"\{[^{}]*input_i[^{}]*\}", r.stderr, re.S)
d = json.loads(m.group(0))
print("1. pruchod:", {k: d[k] for k in ("input_i", "input_tp", "input_lra", "input_thresh")})

# 2. pruchod - presna korekce
meas = (f"{LN}:measured_I={d['input_i']}:measured_TP={d['input_tp']}"
        f":measured_LRA={d['input_lra']}:measured_thresh={d['input_thresh']}"
        f":offset={d['target_offset']}:linear=true")
subprocess.run([FF, "-y", "-v", "error", "-i", wav, "-af",
                f"{PRE},{meas},alimiter=limit=0.94:level=disabled",
                "-ar", "48000", "-ac", "1", "-c:a", "libmp3lame",
                "-b:a", "256k", "-compression_level", "0",
                "-metadata", "title=Sestrih slysitelnych useku",
                "-metadata", "comment=vycisteno a zesileno", mp3], check=True)
print("hotovo:", mp3, os.path.getsize(mp3), "B")
