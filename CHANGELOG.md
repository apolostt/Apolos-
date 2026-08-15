# Changelog

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
