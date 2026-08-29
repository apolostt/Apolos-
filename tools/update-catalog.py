#!/usr/bin/env python3
"""Build compact offline lookup tables from the official IEEE and Bluetooth SIG registries."""

import csv
import io
import pathlib
import re
import urllib.request


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
SOURCES = {
    "oui": "https://standards-oui.ieee.org/oui/oui.csv",
    "companies": "https://bitbucket.org/bluetooth-SIG/public/raw/main/assigned_numbers/company_identifiers/company_identifiers.yaml",
    "services": "https://bitbucket.org/bluetooth-SIG/public/raw/main/assigned_numbers/uuids/service_uuids.yaml",
    "member_services": "https://bitbucket.org/bluetooth-SIG/public/raw/main/assigned_numbers/uuids/member_uuids.yaml",
    "appearances": "https://bitbucket.org/bluetooth-SIG/public/raw/main/assigned_numbers/core/appearance_values.yaml",
}


def download(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "KUBA-Nearby-Scanner-Catalog/1.1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8-sig")


def clean(value: str) -> str:
    value = value.strip().strip("'").strip('"')
    return re.sub(r"[\t\r\n]+", " ", value)


def write_oui(text: str) -> int:
    rows = []
    for row in csv.DictReader(io.StringIO(text)):
        assignment = re.sub(r"[^0-9A-Fa-f]", "", row.get("Assignment", "")).upper()
        organization = clean(row.get("Organization Name", ""))
        if len(assignment) >= 6 and organization:
            rows.append((assignment[:6], organization))
    rows = sorted(dict(rows).items())
    (ASSETS / "ieee_oui.tsv").write_text("".join(f"{key}\t{name}\n" for key, name in rows), encoding="utf-8")
    return len(rows)


def yaml_pairs(text: str, key: str) -> list[tuple[int, str]]:
    value = None
    rows = []
    value_pattern = re.compile(rf"^\s*-\s*{re.escape(key)}:\s*(0x[0-9A-Fa-f]+|\d+)\s*$")
    name_pattern = re.compile(r"^\s*name:\s*(.+?)\s*$")
    for line in text.splitlines():
        match = value_pattern.match(line)
        if match:
            value = int(match.group(1), 0)
            continue
        match = name_pattern.match(line)
        if match and value is not None:
            rows.append((value, clean(match.group(1))))
            value = None
    return rows


def write_simple(name: str, rows: list[tuple[int, str]], hex_width: int = 4) -> int:
    rows = sorted(dict(rows).items())
    (ASSETS / name).write_text(
        "".join(f"{value:0{hex_width}X}\t{label}\n" for value, label in rows), encoding="utf-8"
    )
    return len(rows)


def write_appearances(text: str) -> int:
    rows = []
    category = None
    category_name = None
    sub_value = None
    for line in text.splitlines():
        match = re.match(r"^\s- category:\s*(0x[0-9A-Fa-f]+|\d+)\s*$", line)
        if match:
            category = int(match.group(1), 0)
            category_name = None
            sub_value = None
            continue
        match = re.match(r"^\s{4}name:\s*(.+?)\s*$", line)
        if match and category is not None:
            category_name = clean(match.group(1))
            rows.append((category << 6, category_name))
            continue
        match = re.match(r"^\s{4}- value:\s*(0x[0-9A-Fa-f]+|\d+)\s*$", line)
        if match:
            sub_value = int(match.group(1), 0)
            continue
        match = re.match(r"^\s{6}name:\s*(.+?)\s*$", line)
        if match and category is not None and sub_value is not None:
            label = clean(match.group(1))
            rows.append(((category << 6) | sub_value, f"{category_name or 'Device'} – {label}"))
            sub_value = None
    return write_simple("bt_appearances.tsv", rows)


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    data = {key: download(url) for key, url in SOURCES.items()}
    counts = {
        "IEEE OUI": write_oui(data["oui"]),
        "Bluetooth companies": write_simple("bt_companies.tsv", yaml_pairs(data["companies"], "value")),
        "Bluetooth services": write_simple(
            "bt_services.tsv",
            yaml_pairs(data["services"], "uuid") + yaml_pairs(data["member_services"], "uuid"),
        ),
        "Bluetooth appearances": write_appearances(data["appearances"]),
    }
    print(", ".join(f"{name}: {count}" for name, count in counts.items()))


if __name__ == "__main__":
    main()
