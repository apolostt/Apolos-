# Cisteni a sestrih tichych nahravek

Nastroje pro nahravky, kde je rec hluboko v sumu: vytahnou jen useky,
kde je neco slyset, potlaci sum, vyrovnaji hlasitost a slozi jedno MP3.

## Instalace

```
pip install -r requirements.txt
```

ffmpeg se stahne s balickem `imageio-ffmpeg`, nic dalsiho neni potreba.

## Pouziti

```
python3 build.py <adresar_s_nahravkami> <vystupni_adresar>
python3 encode.py <vystupni_adresar>/sestrih.wav <vystupni_adresar>/vysledek.mp3
```

`build.py` zapise `sestrih.wav` a `usek_report.json` s prehledem toho,
co se z jednotlivych souboru vzalo.

## Jak to funguje

`dsp.py`

- `denoise` - Wienerovo potlaceni sumu ve spektru. Sumove spektrum se
  odhaduje jako 15. percentil vykonu pres cas (sum je stacionarni, rec ne),
  zisk se pocita metodou decision-directed a vyhlazuje se pres frekvenci,
  aby nevznikal "hudebni" sum.
- `shape` - horni propust 90 Hz, dolni 9 kHz, +4 dB kolem 2,6 kHz pro
  srozumitelnost a -4 dB kolem 150 Hz proti dunivosti.
- `find_segments` - usek se bere jen tam, kde energie v pasmu 300-3800 Hz
  prevysi sumove dno v puvodnim (+4 dB) i ve vycistenem (+6 dB) signalu.
  Dve kriteria zaroven odfiltruji pouhe kolisani sumu. Blizke useky se
  spojuji (mezera do 0,7 s) a lemuji se 0,3 s kontextu.

`build.py` kazdy usek zesili na spolecnou uroven (strop +46 dB, aby se
nezvedl jen sum), mekce zkomprimuje, olemuje prolnutim 40 ms a slozi za
sebe s 0,35 s ticha.

`encode.py` provede dvoupruchodovou normalizaci na -16 LUFS s True Peak
-1,5 dBFS a ulozi MP3 256 kbps.

## Poznamka k rnnoise

Filtr `arnndn` (rnnoise) byl vyzkousen a zamitnut: na takto tichem
materialu potlacoval i uzitecny signal o 25-44 dB, tedy mazal prave to,
co je potreba slyset.
