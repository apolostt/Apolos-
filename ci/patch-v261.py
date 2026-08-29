from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")

for rel in ["app/build.gradle", "ci-build.sh", "README.md"]:
    p = root / rel
    s = p.read_text(encoding="utf-8")
    s = s.replace("versionCode 18", "versionCode 19")
    s = s.replace('versionName "2.6.0"', 'versionName "2.6.1"')
    s = s.replace("--version-code 18 --version-name 2.6.0", "--version-code 19 --version-name 2.6.1")
    s = s.replace("kuba-v260.keystore", "kuba-v261.keystore")
    s = s.replace("kubav260", "kubav261")
    s = s.replace("KUBA Nearby Scanner v2.6.0", "KUBA Nearby Scanner v2.6.1")
    s = s.replace("KUBA-Nearby-Scanner-v2.6.0.apk", "KUBA-Nearby-Scanner-v2.6.1.apk")
    s = s.replace("SHA256SUMS-v2.6.0.txt", "SHA256SUMS-v2.6.1.txt")
    s = s.replace("INSTALL-v2.6.0.txt", "INSTALL-v2.6.1.txt")
    s = s.replace("Scanner 2.6.0", "Scanner 2.6.1")
    p.write_text(s, encoding="utf-8")

p = root / "app/src/main/java/com/kuba/nearbyscanner/MainActivity.java"
s = p.read_text(encoding="utf-8")
s = s.replace("KUBA Nearby Scanner 2.6.0\\n", "KUBA Nearby Scanner 2.6.1\\n")
old = '''            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {\n                setStatus("Bluetooth dokončen • probíhá síťová detekce");\n'''
new = '''            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {\n                if (scanning && bluetoothAdapter != null\n                        && System.currentTimeMillis() - scanStartedAt < SCAN_DURATION_MS - 5000L) {\n                    handler.postDelayed(() -> {\n                        try {\n                            if (scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {\n                                boolean restarted = bluetoothAdapter.startDiscovery();\n                                if (restarted) setStatus("Bluetooth Classic • další průchod pro telefony/Galaxy");\n                            }\n                        } catch (SecurityException ignored) { }\n                    }, 700);\n                } else {\n                    setStatus("Bluetooth dokončen • probíhá síťová detekce");\n                }\n'''
if old not in s:
    raise RuntimeError("MainActivity discovery block not found")
s = s.replace(old, new)
p.write_text(s, encoding="utf-8")

p = root / "app/src/main/java/com/kuba/nearbyscanner/DeviceCatalog.java"
s = p.read_text(encoding="utf-8")
old = '''    private void applyProfileSignature(BluetoothIdentity out) {\n        String evidence = out.searchableText().toLowerCase(Locale.ROOT);\n        for (NameSignature signature : profileSignatures) {\n            if (!signature.matches(evidence)) continue;\n            if (out.vendor.isEmpty()) out.vendor = signature.brand;\n            if (out.modelHint.isEmpty() || out.modelHint.startsWith("Zařízení výrobce") || out.modelHint.startsWith("Přesný model")) {\n                out.modelHint = signature.model;\n            }\n            setCategory(out, signature.category, signature.confidence, signature.reason);\n            return;\n        }\n    }\n'''
new = '''    private void applyProfileSignature(BluetoothIdentity out) {\n        String evidence = out.searchableText().toLowerCase(Locale.ROOT);\n        NameSignature best = null;\n        for (NameSignature signature : profileSignatures) {\n            if (!signature.matches(evidence)) continue;\n            if (best == null || signature.confidence > best.confidence) best = signature;\n        }\n        if (best == null) return;\n        if (out.vendor.isEmpty()) out.vendor = best.brand;\n        if (out.modelHint.isEmpty() || out.modelHint.startsWith("Zařízení výrobce") || out.modelHint.startsWith("Přesný model")) {\n            out.modelHint = best.model;\n        }\n        setCategory(out, best.category, best.confidence, best.reason);\n    }\n'''
if old not in s:
    raise RuntimeError("DeviceCatalog profile method not found")
s = s.replace(old, new)
p.write_text(s, encoding="utf-8")

p = root / "app/src/main/assets/device_name_signatures.tsv"
s = p.read_text(encoding="utf-8")
line = "galaxy z flip7 fe|galaxy z flip 7 fe|flip7 fe|flip 7 fe|sm-f761|sm-f761b\tSamsung\tSamsung Galaxy Z Flip7 FE (SM-F761B)\tPHONE\t99\tPřesný název/model odpovídá Samsung Galaxy Z Flip7 FE\n"
needle = "galaxy s|galaxy a|galaxy z|galaxy note|galaxy tab|samsung phone|one ui|sm-g|sm-a|sm-s|sm-f|sm-n\tSamsung\tSamsung Galaxy telefon nebo tablet\tPHONE\t92"
if "sm-f761b\tSamsung\tSamsung Galaxy Z Flip7 FE" not in s:
    idx = s.find(needle)
    if idx < 0:
        raise RuntimeError("generic Samsung name signature not found")
    s = s[:idx] + line + s[idx:]
p.write_text(s, encoding="utf-8")

p = root / "app/src/main/assets/device_profile_signatures.tsv"
s = p.read_text(encoding="utf-8")
s = s.replace(
    "samsung electronics|galaxy watch|smarttag|fd5a|fd4b\tSamsung\tSamsung zařízení: Galaxy Watch, Buds, SmartTag nebo telefon\tWEARABLE\t82\tSamsung výrobce/služba v BLE reklamě; přesný typ určuje název/služby",
    "samsung electronics|samsung electro-mechanics\tSamsung\tSamsung Galaxy / SmartThings zařízení\tPHONE\t76\tSamsung výrobce v BLE reklamě; přesný model určuje název, služba nebo modelový prefix"
)
line = "galaxy z flip7 fe|galaxy z flip 7 fe|flip7 fe|flip 7 fe|sm-f761|sm-f761b\tSamsung\tSamsung Galaxy Z Flip7 FE (SM-F761B)\tPHONE\t99\tPřesný Samsung modelový prefix SM-F761 / název Flip7 FE\n"
needle = "samsung electronics|samsung electro-mechanics|smartthings|one ui|galaxy s|galaxy a|galaxy z|galaxy note|galaxy tab|sm-g|sm-a|sm-s|sm-f|sm-n\tSamsung\tSamsung Galaxy telefon/tablet podle výrobce, názvu nebo prefixu\tPHONE\t88"
if "sm-f761b\tSamsung\tSamsung Galaxy Z Flip7 FE" not in s:
    idx = s.find(needle)
    if idx < 0:
        raise RuntimeError("generic Samsung profile signature not found")
    s = s[:idx] + line + s[idx:]
p.write_text(s, encoding="utf-8")

(root / "CHANGELOG-v2.6.1.md").write_text(
    "# KUBA Nearby Scanner v2.6.1\n\n"
    "STATUS: TEST CANDIDATE\n\n"
    "- Oprava Samsung klasifikace: vybírá se nejsilnější profilová shoda.\n"
    "- Přidán přesný profil Samsung Galaxy Z Flip7 FE / SM-F761B.\n"
    "- Bluetooth Classic discovery se během 60s skenu automaticky opakuje.\n",
    encoding="utf-8",
)

print("v2.6.1 patch applied")
