#!/usr/bin/env python3
"""Generate Quiet TV's original, softly rounded 65 ms navigation tone."""
import math
import struct
import wave
from pathlib import Path

rate = 44100
length = .065
frames = []
for i in range(round(rate * length)):
    t = i / rate
    attack = .5 - .5 * math.cos(math.pi * min(t / .004, 1))
    release = min((length - t) / .012, 1) ** 2
    envelope = attack * math.exp(-t / .015) * release
    # A warm low pluck with a faint upper partial; no noise or abrupt transient.
    tone = math.sin(2 * math.pi * (620*t - 900*t*t)) + .12 * math.sin(2*math.pi*1240*t)
    frames.append(round(32767 * .34 * envelope * tone))
output = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw/home_navigation.wav'
with wave.open(str(output), 'wb') as sound:
    sound.setnchannels(1)
    sound.setsampwidth(2)
    sound.setframerate(rate)
    sound.writeframes(struct.pack('<' + 'h' * len(frames), *frames))
print(f'{output.name}: {len(frames)/rate:.3f}s, {output.stat().st_size} bytes, peak {max(map(abs,frames))/32767:.3f}')
