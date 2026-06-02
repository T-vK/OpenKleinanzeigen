#!/usr/bin/env python3
"""Build index-v1.json without androguard APK scanning (AGP 35 resources break it)."""
from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path


def run(cmd: list[str]) -> str:
    return subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT).strip()


def parse_badging(badging: str) -> dict[str, str]:
    out: dict[str, str] = {}
    m = re.search(
        r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'",
        badging,
    )
    if not m:
        raise ValueError(f"Could not parse aapt badging:\n{badging[:500]}")
    out["packageName"] = m.group(1)
    out["versionCode"] = m.group(2)
    out["versionName"] = m.group(3)
    sm = re.search(r"sdkVersion:'(\d+)'", badging)
    tm = re.search(r"targetSdkVersion:'(\d+)'", badging)
    out["minSdkVersion"] = sm.group(1) if sm else "26"
    out["targetSdkVersion"] = tm.group(1) if tm else "35"
    return out


def main() -> None:
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <apk-path> <fdroid-dir>", file=sys.stderr)
        sys.exit(1)

    apk_src = Path(sys.argv[1]).resolve()
    fdroid_dir = Path(sys.argv[2]).resolve()
    repo_dir = fdroid_dir / "repo"
    config_path = fdroid_dir / "config.yml"
    metadata_dir = fdroid_dir.parent / "metadata" / "en-US"

    if not apk_src.is_file():
        raise SystemExit(f"APK not found: {apk_src}")

    aapt = os.environ.get("AAPT")
    if not aapt:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk:
            raise SystemExit("Set ANDROID_HOME or AAPT")
        build_tools = sorted(Path(sdk, "build-tools").glob("*"), key=lambda p: p.name)
        if not build_tools:
            raise SystemExit("No build-tools in ANDROID_HOME")
        aapt = str(build_tools[-1] / "aapt")

    badging = run([aapt, "dump", "badging", str(apk_src)])
    info = parse_badging(badging)
    package = info["packageName"]
    version_code = int(info["versionCode"])
    version_name = info["versionName"]
    apk_name = f"{package}_{version_code}.apk"

    repo_dir.mkdir(parents=True, exist_ok=True)
    apk_dest = repo_dir / apk_name
    shutil.copy2(apk_src, apk_dest)

    sha256 = hashlib.sha256(apk_dest.read_bytes()).hexdigest()
    now_ms = int(time.time() * 1000)

    # Certificate metadata via fdroidserver (no resources.arsc scan).
    sys.path.insert(0, "")
    os.chdir(fdroid_dir)
    from fdroidserver import common  # noqa: WPS433
    from fdroidserver.update import getsig  # noqa: WPS433

    sig = getsig(str(apk_dest))
    cert = common.get_first_signer_certificate(str(apk_dest))
    signer = common.signer_fingerprint(cert)

    summary = (metadata_dir / "summary.txt").read_text().strip() if (metadata_dir / "summary.txt").exists() else "OpenKleinanzeigen"
    description = (metadata_dir / "description.txt").read_text().strip() if (metadata_dir / "description.txt").exists() else summary
    anti_raw = (metadata_dir / "antiFeatures.txt").read_text().strip() if (metadata_dir / "antiFeatures.txt").exists() else "NonFreeNet"
    anti_features = [x.strip() for x in anti_raw.split(",") if x.strip()]

    repo_url = "https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo"
    if config_path.exists():
        for line in config_path.read_text().splitlines():
            if line.startswith("repo_url:"):
                repo_url = line.split(":", 1)[1].strip()
                break

    index = {
        "repo": {
            "timestamp": now_ms,
            "version": 20002,
            "name": "OpenKleinanzeigen",
            "icon": "icon.png",
            "address": repo_url,
            "description": "Custom repository for OpenKleinanzeigen",
        },
        "apps": [
            {
                "categories": ["Internet"],
                "suggestedVersionName": version_name,
                "suggestedVersionCode": str(version_code),
                "description": description,
                "license": "AGPL-3.0-or-later",
                "summary": summary,
                "webSite": "https://github.com/T-vK/OpenKleinanzeigen",
                "added": now_ms,
                "packageName": package,
                "lastUpdated": now_ms,
                "localized": {"en-US": {"name": "OpenKleinanzeigen"}},
                **({"antiFeatures": anti_features} if anti_features else {}),
            }
        ],
        "packages": {
            package: [
                {
                    "added": now_ms,
                    "apkName": apk_name,
                    "hash": sha256,
                    "hashType": "sha256",
                    "minSdkVersion": int(info["minSdkVersion"]),
                    "packageName": package,
                    "sig": sig,
                    "signer": signer,
                    "size": apk_dest.stat().st_size,
                    "targetSdkVersion": int(info["targetSdkVersion"]),
                    "versionCode": version_code,
                    "versionName": version_name,
                }
            ]
        },
    }

    index_path = repo_dir / "index-v1.json"
    index_path.write_text(json.dumps(index, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {index_path}")

    # Sign index (JAR + GPG) using fdroidserver.
    subprocess.check_call(["fdroid", "signindex"], cwd=fdroid_dir)
    print("Signed repo index with fdroid signindex")


if __name__ == "__main__":
    main()
