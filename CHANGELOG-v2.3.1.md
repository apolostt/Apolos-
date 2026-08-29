# KUBA Nearby Scanner v2.3.1

STATUS: VERIFIED BUILD

## Opravy chyb

- **Uložená / spárovaná zařízení se už nezobrazují, dokud nejsou v dosahu.**
  Dříve se známá spárovaná Bluetooth zařízení uložená v telefonu po chvíli
  chybně označila jako „v dosahu", i když nevysílala žádný živý signál.
  Příčinou bylo, že prořezávací smyčka počítala stav dosahu jen z času posledního
  zápisu, který se u spárovaného snímku nastavoval na aktuální čas.
- Nově má každé zařízení příznak potvrzeného živého signálu (`signalConfirmed`).
  V dosahu a v seznamu se ukáže pouze zařízení, u kterého byl skutečně zachycen
  živý BLE / Bluetooth / Wi‑Fi / síťový paket. Uložený záznam z paměti telefonu
  se drží skrytý pro pozdější obohacení dat, ale sám o sobě se nikdy nezobrazí.

## Vylepšení

- Do agresivního BLE skenu byl přidán `setLegacy(false)`, takže se zachytí
  i zařízení vysílající přes rozšířenou reklamu (BLE 5 / extended advertising),
  nejen starší legacy pakety. Sken tak pokryje širší okruh okolních zařízení.

## Sestavení / nástroje

- Build skript (`build-apk.sh`) nově dexuje přes samostatný D8 z Google Maven
  (R8/D8 8.9.35). D8 dodávaný v build‑tools r34 (8.2.2) padal na bytecode
  z novějších JDK (21+) chybou `NullPointerException` v anonymních třídách.
  Build je díky tomu reprodukovatelný na aktuálních JDK.

Ověření: Android API 34 build, minSdk 26, targetSdk 34, APK Signature Scheme
v2/v3. Reálnou stabilitu rádia a spotřebu je potřeba změřit na konkrétním
telefonu, protože výrobci Androidu používají rozdílné úsporné režimy.
