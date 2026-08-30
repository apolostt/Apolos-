# KUBA Navigace CR

Moderní česká navigace pro Android postavená jako hybridní aplikace — nativní
Android obal (GPS služba, hlas, výpočet trasy přes Valhallu, road-info) + webové
rozhraní s mapou **MapLibre GL** a vektorovými dlaždicemi OpenFreeMap.

Tento repozitář obsahuje **editovatelné zdroje webové vrstvy** (`web/`), nativní
skořápku (`tools/base.apk`) a build skript, který z nich složí a podepíše
kompletní instalovatelné APK.

> Sestavené APK cílí na **Android 16 (API 36)**, `minSdk 28` — plně kompatibilní
> se **Samsung Galaxy Z Fold 7** i staršími telefony od Androidu 9.

---

## Co je nové v této verzi

Vylepšení se soustředí na plynulost jízdy, čitelnost navádění a vzhled mapy:

### 🚗 Plynulý živý pohyb auta
- Nový **prediktivní render-loop** (`requestAnimationFrame`), který mezi
  jednotlivými GPS fixy dopočítává polohu auta podél trasy (dead-reckoning:
  poloha += rychlost × čas). Auto se tak pohybuje **plynule i při GPS 1×/s**,
  místo trhaného poskakování z bodu do bodu.
- Rychlost se odhaduje i tehdy, když ji přístroj nehlásí (z posunu po trase).
- Kamera sleduje **vyhlazenou** polohu každý snímek a směr se plynule stáčí
  (žádné skoky natočení).

### 📏 Odpočet metrů k odbočení
- Vzdálenost k dalšímu manévru se přepočítává **každý snímek**, takže metry
  **plynule odtikávají** (320 → 315 → 310 …) místo skoku po GPS fixu.
- Jemné zaokrouhlování: po 5 m zblízka, po 10/25 m dál, kilometry v dálce,
  a **„TEĎ“** přímo v místě odbočení.
- Nový **ukazatel přiblížení** (progress bar) pod pokynem, který se plní, jak se
  blížíš k odbočce, se stavy *přibližování* (zelená) a *hned odbočit* (žlutá,
  pulzování šipky).

### 🗺️ Textury mapy a auta
- **3D budovy**: stínování podle výšky, svislý gradient, dřívější náběh
  (od zoomu 14) a jemné „vyrůstání“ budov při přibližování.
- **Atmosféra**: směrové světlo scény (`setLight`) a denní obloha (`setSky`)
  pro reálnou hloubku 3D.
- **Auto (marker)**: přidaný **světelný kužel reflektorů** projektovaný před
  jedoucí auto při navigaci.

### 📡 Lepší napojení na GPS
- Klientský filtr fixů: **potlačení odlehlých „teleportů“** u nepřesných poloh
  a **dopočet směru z pohybu**, když ho čidlo nehlásí (typicky při pomalé jízdě).

Vše ostatní (vyhledávání, oblíbená místa, historie jízd, sdílení polohy,
offline mapy, satelitní panel) zůstává funkčně zachováno.

---

## Struktura projektu

```
web/                     Editovatelné zdroje webové vrstvy
  index.html             UI navigace
  app.js                 Logika (mapa, navigace, GPS, HUD) — zde jsou vylepšení
  styles.css             Vzhled
  vendor/                MapLibre GL JS (v5) + licence
tools/
  base.apk               Původní nativní skořápka (classes.dex, manifest, res)
  build-apk.sh           Sestaví a podepíše APK z web/ + base.apk
  kuba-release.keystore  Self-signed release klíč (viz poznámka níže)
  uber-apk-signer-*.jar  Zipalign + apksigner (v1/v2/v3) — bez Android SDK
dist/
  KUBA-Navigace-CR-v1.9.1-Fold7-Android16-signed.apk   Hotové podepsané APK
```

---

## Sestavení

```bash
bash tools/build-apk.sh
```

Potřebuje jen `bash`, `zip`, `unzip` a Javu (JDK 8+). Zipalign i podpis zajišťuje
přibalený `uber-apk-signer`, takže **není potřeba Android SDK**. Výsledek se
objeví v `dist/`.

Chceš-li měnit chování aplikace, uprav soubory v `web/` a spusť build znovu.

## Instalace do telefonu (Samsung Fold 7)

1. Přenes `dist/KUBA-Navigace-CR-v1.9.1-Fold7-Android16-signed.apk` do telefonu.
2. Otevři soubor ve Správci souborů a potvrď instalaci.
3. Při první instalaci povol *„Instalovat neznámé aplikace“* pro danou appku.

> Pokud máš nainstalovanou původní verzi podepsanou jiným klíčem, nejdřív ji
> odinstaluj — tento build je podepsaný novým vlastním klíčem, takže se
> nenainstaluje „přes“ starou instalaci.

### Poznámka ke klíči

`tools/kuba-release.keystore` je **self-signed klíč pro osobní distribuci**
(heslo úložiště i klíče: `kubanav2026`, alias `kuba`). Ponech ho, pokud chceš,
aby se příští buildy instalovaly jako aktualizace přes tento. Není to nahrávací
klíč pro Google Play; při případném zveřejnění ho drž v soukromí / rotuj.
