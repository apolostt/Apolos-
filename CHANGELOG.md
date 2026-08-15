# Changelog

## v1.9.1 (4) — zeleň pod silnice, realistické stromy, Octavia, směrová cedule, plynulejší kamera, offline

Reakce na test na reálné mapě (celá mapa byla zelená, textura stromů kostkovaná).

### Opraveno
- **Zeleň už nepřekrývá silnice a budovy**: lesy/tráva/parky se vkládají POD
  silniční vrstvu (`firstLine`), ne nad ni — cesty a domy jsou zase vidět.
- **Realistická textura stromů**: měkké nepravidelné koruny s vlastními stíny
  místo kostkované mřížky.

### Přidáno / změněno
- **Model auta jako Škoda Octavia**: přerýsovaný stříbrný liftback (střecha,
  zadní okno, chromová lišta, C-koncová světla, SPZ) — pohled shora i zezadu.
- **Plynulejší jízda**: kamera nově vyhlazuje polohu i směr každý snímek přes
  `jumpTo` (dřív trhaný throttled `easeTo`).
- **Směrová cedule nahoře** (`guideSign`): zelená dálniční cedule s cílovým
  městem, šipkou a kolika km — jako navádění na dálnici.
- **Chodníky mírně vyvýšené**: světlý chodník s obrubníkovým stínem a nízká
  vyvýšená dlažba u pěších zón, aby byl vidět rozdíl.
- **Offline jízda**: když vypadne signál, navigace pokračuje podle naplánované
  trasy (vše běží v telefonu); přepočet se offline přeskočí s hláškou místo chyby.

---

## v1.9.1 (3) — čistá fasáda + střecha, terén (kopce), textura stromů, realističtější auto

Reakce na test na reálné mapě.

### Opraveno / změněno
- **Okna už nejsou na střeše**: fasádní textura přepracovaná (čistá mřížka oken
  bez opakujících se dveří) + nová **plná střešní čepička** (`kuba-building-roof`),
  takže vzor oken nikdy neprosakuje na střechu.
- **Terén / kopce**: přidán výškový DEM (`raster-dem`, terrarium) + 3D terén
  (`setTerrain`) a **hillshade**, takže kopce a klesání jsou vidět. Vše defenzivně
  ošetřené — když DEM není dostupný, mapa zůstane placatá a nic se nerozbije.
- **Stromy mají texturu** (koruny), ne jen zelenou barvu — dlaždicová textura přes
  `fill-pattern`/`fill-extrusion-pattern` (`ensureTreeImage`).
- **Realističtější model auta** (zadní pohled): střecha, zadní okno, dvě koncová
  světla, SPZ, jemné odlesky.

---

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
