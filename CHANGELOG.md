# Changelog

## v1.9.1 (2) — okna na barácích, lesy, okraje silnic

### Přidáno
- **Okna a dveře na 3D budovách**: procedurální fasádní textura (`ensureFacadeImage`)
  nanesená přes `fill-extrusion-pattern`; budovy jsou nově **neprůhledné**.
- **Lesy a stromy**: zelené plochy z živých OpenMapTiles dat (`landcover`/`park`)
  + nízká zelená extruze jako **3D koruny stromů** (`kuba-tree-canopy`).
- **Okraje silnic (levá/pravá)**: casing + výplň + přerušovaná středová čára
  (`kuba-road-edge`/`-fill`/`-centre`), rozměry laděné tak, aby se **auto vešlo**.
- **Jména obchodů/podniků** hustěji a od nižšího zoomu — živě z OSM dlaždic.
- **Automaticky přesné GPS** při navigaci (`forcePreciseLocation`) — OS zapojí
  co nejvíc GNSS konstelací (GPS+Galileo+GLONASS+BeiDou) pro nejlepší přesnost.
- Lepší model semaforu (záře a lesk lamp).

### Změněno
- Model auta zmenšen a vyladěn, aby se vešel do jízdního pruhu; kovovější vzhled
  a kontaktní stín. Náběh kamery mírně blíž (zoom 19,1).
- `setBuildingAppearance` nově respektuje jen budovy (nepřebarvuje koruny stromů).

### Poznámky k limitům
- Živá jména **z Googlu** by vyžadovala placený API klíč a úpravu nativního
  jádra — použita jsou proto živá OSM data (fungují i offline z uložených map).
- Výběr satelitů řídí OS/nativní vrstva; web umí jen vynutit nejpřesnější profil.
- Stažení offline map je nativní funkce a v aplikaci už existuje (Mapové nástroje).

---

## v1.9.1 — plynulejší jízda, čitelnější navádění, bohatší mapa

Vylepšení webové vrstvy (nativní skořápka beze změny, stále targetSdk 36 / Android 16).

### Přidáno
- Prediktivní render-loop (`predictTick`) pro plynulý pohyb auta mezi GPS fixy
  s dead-reckoningem podél trasy a vyhlazeným sledováním kamery.
- Plynulý odpočet metrů k odbočení (`renderTurnGuidance` volaný každý snímek) +
  ukazatel přiblížení (progress bar) se stavy „přibližování“ a „hned odbočit“.
- Jemné zaokrouhlování vzdálenosti (`countdownLabel`) včetně stavu „TEĎ“.
- Atmosféra mapy: směrové světlo (`setLight`) a denní obloha (`setSky`)
  přes `setupMapAtmosphere`.
- Světelný kužel reflektorů markeru auta při jízdě.
- Klientský GPS filtr (`refineFix`): potlačení odlehlých skoků a dopočet směru
  z pohybu.

### Změněno
- 3D budovy: stínování podle výšky, svislý gradient, náběh od zoomu 14
  a „vyrůstání“ při přiblížení.
- Odhad rychlosti z posunu po trase, když ji GPS nehlásí.

### Build
- `tools/build-apk.sh`: reprodukovatelné sestavení a podpis (v1+v2+v3) bez
  Android SDK pomocí přibaleného uber-apk-signeru.
