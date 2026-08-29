# KUBA Nearby Scanner v2.1.0

- Opraveno zobrazování starých/uložených zařízení: zařízení mimo aktuální dosah se v seznamu nezobrazují.
- Přidán automatický úklid mimo dosah pro Bluetooth, Wi-Fi, mDNS, SSDP a ONVIF nálezy.
- Spárovaná Bluetooth zařízení už sama o sobě nevytváří falešný nález v okolí; zobrazí se až po zachycení signálu.
- Výchozí řazení seznamu je podle blízkosti, nejbližší položky jsou nahoře.
- Skenování pokračuje v klidových oknech i při otevřeném detailu zařízení.
- BLE live tracking má kompatibilní fallback, když telefon nepodporuje agresivní/all-PHY nastavení.
- Wi-Fi se po hlavním skenu průběžně obnovuje i bez otevřeného detailu.
- Levé menu má rozbalovací skupinu značek zařízení.
- Samsung filtr nově zahrnuje Galaxy telefony, tablety, Watch, Fit, Ring, Buds a SmartTag.
- Rozšířena offline podpisová databáze o Samsung modelové prefixy SM-G, SM-A, SM-S, SM-F, SM-N a SM-R.
- Rozšířena offline databáze o Google Pixel, Fast Pair, Xiaomi/Redmi/Mijia, Samsung SmartThings a další BLE profily.
- Detail zařízení ukazuje stav v dosahu, první a poslední zachycení, automatické mazání, možné typy a signálovou poznámku.
- Počítadla a brand menu počítají jen aktuálně zobrazitelná zařízení, ne staré schované položky.

Poznámka: směr a metry jsou pořád odhady podle RSSI, pohybu telefonu, kompasu/GPS a vysílaných dat. Android neumí z běžného Bluetooth skenu získat skutečný úhel příchodu signálu, pokud telefon a zařízení nepodporují speciální direction-finding hardware.
