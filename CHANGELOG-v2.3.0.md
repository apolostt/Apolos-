# KUBA Nearby Scanner v2.3.0

STATUS: VERIFIED BUILD

- Přidána samostatná Android foreground služba pro BLE hlídání po minimalizaci aplikace.
- Služba skenuje v řízených oknech 11 sekund a 7 sekund odpočívá, aby omezila spotřebu a počet startů BLE skeneru.
- Trvalé oznámení ukazuje počet zachycených paketů a poslední zařízení; obsahuje tlačítko Zastavit.
- Označené Bluetooth zařízení upozorní při návratu i tehdy, když hlavní obrazovka není viditelná.
- Aktivita a služba se automaticky střídají, takže neběží dva běžné BLE skeny současně.
- Raw reklama, název, MAC a RSSI z background služby se předávají zpět do detailu zařízení.
- Nastavení ukazuje poslední background cyklus, poslední nález, celkový počet paketů a poslední chybu skeneru.
- Přidán přepínač pro zapnutí/vypnutí BLE hlídání na pozadí.
- Při odmítnutí rozšířeného PHY se služba automaticky přepne na kompatibilní BLE režim.

Ověření: Android API 34 build, minSdk 26, APK Signature Scheme v2/v3. Reálnou stabilitu rádia a spotřebu je potřeba změřit na konkrétním telefonu, protože výrobci Androidu používají rozdílné úsporné režimy.
