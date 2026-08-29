# KUBA Nearby Scanner

Nativní Android aplikace pro skenování okolních Bluetooth/BLE zařízení, Wi‑Fi přístupových bodů a zařízení, která se v lokální síti sama hlásí přes SSDP nebo ONVIF WS‑Discovery.

## Co umí

- Bluetooth Classic a Bluetooth Low Energy sken.
- Automatický přechod z rozšířeného BLE režimu na kompatibilní sken, pokud jej Bluetooth čip telefonu odmítne.
- Samostatná foreground služba pro cyklické BLE hlídání po minimalizaci aplikace, včetně trvalého systémového oznámení a tlačítka Zastavit.
- Oddělené režimy skenování: při otevřené aplikaci běží detailní/presence sken, na pozadí pouze služba, takže se BLE scany zbytečně nepřekrývají.
- Diagnostika posledního cyklu služby, posledního nálezu, počtu paketů a poslední chyby přímo v Nastavení.
- Důkladný 60sekundový BLE sken v režimu nízké latence pro krátce vysílající zařízení.
- BLE sken přes všechna podporovaná PHY rádia telefonu, včetně LE 1M/2M/Coded tam, kde to hardware a Android povolí.
- Řazení seznamu podle kategorie nebo podle odhadované vzdálenosti v metrech, včetně přímé volby v menu.
- Samostatná kategorie a filtr pro chytré hodinky, fitness náramky a sportovní senzory.
- Rozpoznání zvukových Bluetooth zařízení podle třídy a názvu.
- Wi‑Fi sken se sílou signálu, frekvencí a typem zabezpečení.
- Kontrola stáří Wi‑Fi výsledků, aby se stará síť nezobrazovala jako právě dostupná.
- GPS/GNSS obrazovka se seznamem viditelných satelitů, stavem fixu, konstelací, SVID, C/N0, elevací, azimutem, frekvencí, almanachem a efemeridami.
- Orientační vzdálenost Bluetooth a Wi‑Fi podle RSSI.
- Pasivní síťová detekce ONVIF kamer a UPnP/SSDP zařízení.
- Rozpoznávací pravidla pro známé názvy kamer a zvukových zařízení.
- Kompletní offline registr 39 902 IEEE OUI výrobců a Bluetooth SIG katalog firem, služeb a typů.
- Lokální slovník názvů pro Apple Watch, Galaxy Watch, Garmin, Fitbit, Amazfit/Zepp, Xiaomi/Mi Band, Huawei/Honor, AirTag, SmartTag, Tile a běžná Bluetooth sluchátka.
- Detekce možných trackerů AirTag, SmartTag, Tile, Chipolo, Pebblebee a dalších podle vysílání.
- Levá vysouvací nabídka pro Bluetooth, Wi‑Fi, kamery, zvuk, trackery, značky hodinek, smart-home, koloběžky, telefony, počítače a lokální síť.
- Zobrazovací menu značek a typů zařízení s počty nalezených položek.
- Moderní rolovací menu se samostatnými sekcemi pro skryté názvy a pojmenovaná zařízení.
- 3D radarová mapa s přepínačem Vše / Bluetooth / Wi‑Fi / Skryté a orientační vzdáleností.
- Celoobrazovkový radar: tažení jedním prstem, pinch zoom, oddálení a dvojklik pro reset pohledu.
- mDNS/DNS‑SD sken služeb HTTP/HTTPS, RTSP, Chromecast, AirPlay, tiskárny, HomeKit, Matter, SMB, chytré domácnosti a pracovních stanic.
- Volitelná online aktualizace veřejných IEEE registrů MA‑L, MA‑M a MA‑S přímo do telefonu; nalezené MAC adresy se nikam neposílají.
- Bezpečnostní kontrola telefonu: síť, VPN, privátní DNS, souhrnný provoz, citlivá oprávnění aplikací, Usnadnění, přístup k oznámením a ladění USB.
- Rozšířená identifikace malých IoT modulů, smart-home zařízení, koloběžek/e-bike modulů a platforem Raspberry Pi, ESP32/ESP8266, Nordic nRF, Arduino, Tuya a kamerových či zvukových čipsetů.
- Detail po klepnutí: plynulý živý kompasový ukazatel, šipka podle nejsilnějšího RSSI směru, pomocné GPS vyhodnocení pohybu, vyhlazený odhad metrů, kvalita odhadu, výrobce, modelový odhad, MAC/IP, služby, třída, kanál, zabezpečení, Wi‑Fi standard a raw BLE/Wi‑Fi data.
- Bluetooth/BLE položky se drží v seznamu, dokud se v dosahu znovu hlásí; po delší době bez signálu se označí jako mimo dosah.
- Označené Bluetooth a Wi‑Fi položky lze hlídat na pozadí s upozorněním při znovuobjevení v okolí.
- Filtry: vše, trackery, hodinky/náramky, značky hodinek, možné kamery, spy/sledování, zvuk, koloběžky, chytrá domácnost, telefony, počítače, Wi‑Fi, Bluetooth a síť.
- Funguje bez účtu, cloudu a ukládání dat mimo telefon.

## Reálné limity

- RSSI vzdálenost je pouze odhad; zdi, tělo, anténa a odrazy ji výrazně mění.
- Wi‑Fi sken ukazuje přístupové body. Klienty ve Wi‑Fi lze najít jen tehdy, když odpovědí přes podporované lokální protokoly.
- Skrytou, vypnutou, kabelovou nebo nekomunikující kameru telefon spolehlivě nepotvrdí.
- Označení „možná kamera“ podle názvu má nižší jistotu než přímá ONVIF odpověď.
- Android může omezit četnost Wi‑Fi skenu a vyžaduje zapnuté určování polohy.
- Úplně skryté Bluetooth zařízení lze najít jen tehdy, když zrovna vysílá reklamu, odpoví na discovery nebo se jinak v okolí hlásí.
- Radar z RSSI odhaduje vzdálenost, nikoli skutečný směr; rozmístění bodů je vizualizace.
- Bez rootu nebo lokální VPN Android nedovolí číst obsah komunikace ostatních aplikací ani přesně určit jejich jednotlivý provoz. Kontrola ukazuje souhrnná data a riziková oprávnění, ne automatický důkaz špehování.
- Nález Raspberry Pi, ESP nebo jiného malého modulu sám o sobě neznamená kameru, mikrofon ani tracker.

## Instalace APK

1. Přenes `KUBA-Nearby-Scanner-v2.3.0.apk` do telefonu.
2. V Androidu povol instalaci neznámých aplikací pro aplikaci, ze které APK otevíráš.
3. Nainstaluj a při prvním spuštění povol okolní zařízení, Wi‑Fi a polohu.

## Sestavení bez GitHubu

V Linuxu s Java 17, `curl`, `unzip` a `zip` spusť:

```bash
chmod +x build-apk.sh
./build-apk.sh
```

Skript stáhne oficiální Android API 34 a Build Tools 34 do dočasné složky a vytvoří podepsané testovací APK v `dist/`.

Offline identifikační databáze se obnoví příkazem `python3 tools/update-catalog.py`. Skript používá veřejné oficiální registry IEEE a Bluetooth SIG.

Projekt lze také otevřít v Android Studiu jako běžný projekt. Pro publikaci vytvoř vlastní release signing key; přiložený APK používá oddělený testovací podpis.

## Soukromí

Aplikace neposílá výsledky na internet. Oprávnění `INTERNET` používá jen k lokálním HTTP popisům zařízení nalezených přes SSDP; přijme pouze privátní nebo lokální IP adresy.
