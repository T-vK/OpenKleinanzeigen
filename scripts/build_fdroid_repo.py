#!/usr/bin/env python3
"""Build a complete F-Droid binary repo (index.html, QR, icons, signed JAR)."""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import time
import sys
import zipfile
from pathlib import Path


def run(cmd: list[str], cwd: Path | None = None) -> str:
    return subprocess.check_output(cmd, text=True, stderr=subprocess.STDOUT, cwd=cwd).strip()


def find_aapt() -> str:
    aapt = os.environ.get("AAPT")
    if aapt:
        return aapt
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        raise SystemExit("ANDROID_HOME is not set")
    tools = sorted(Path(sdk, "build-tools").glob("*"), key=lambda p: p.name)
    if not tools:
        raise SystemExit("No Android build-tools found")
    return str(tools[-1] / "aapt")


def parse_badging(badging: str) -> dict:
    m = re.search(
        r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'",
        badging,
    )
    if not m:
        raise ValueError(f"Could not parse aapt badging:\n{badging[:400]}")
    info = {
        "packageName": m.group(1),
        "versionCode": int(m.group(2)),
        "versionName": m.group(3),
    }
    sm = re.search(r"sdkVersion:'(\d+)'", badging)
    tm = re.search(r"targetSdkVersion:'(\d+)'", badging)
    info["minSdkVersion"] = int(sm.group(1)) if sm else 26
    info["targetSdkVersion"] = int(tm.group(1)) if tm else 35
    nm = re.search(r"application-label:'([^']*)'", badging)
    info["name"] = nm.group(1) if nm else "OpenKleinanzeigen"
    return info


def extract_icons_src(apk_path: Path) -> dict[str, str]:
    """Map density -> temp path for the best launcher icon in the APK."""
    patterns = (
        r"res/mipmap-([a-z0-9]+)/ic_launcher(?:_foreground)?\.png$",
        r"res/drawable-([a-z0-9]+)/ic_launcher(?:_foreground)?\.png$",
    )
    density_order = {
        "xxxhdpi": 6,
        "xxhdpi": 5,
        "xhdpi": 4,
        "hdpi": 3,
        "mdpi": 2,
        "ldpi": 1,
    }
    found: dict[str, Path] = {}
    with zipfile.ZipFile(apk_path) as zf:
        for name in zf.namelist():
            for pat in patterns:
                m = re.match(pat, name)
                if not m:
                    continue
                density = m.group(1)
                rank = density_order.get(density, 0)
                if rank >= density_order.get(found.get("_rank", ""), -1) if "_rank" in found else True:
                    tmp = Path("/tmp") / f"ok_icon_{density}.png"
                    tmp.write_bytes(zf.read(name))
                    found = {"_rank": density, density: str(tmp)}
    if not found:
        return {}
    found.pop("_rank", None)
    return found


def patch_scan_apk_aapt_only() -> None:
    from fdroidserver import common
    from fdroidserver.update import getsig, scan_apk as original_scan_apk

    aapt = find_aapt()

    def scan_apk_aapt(apk_file: str, require_signature: bool = True) -> dict:
        badging = run([aapt, "dump", "badging", apk_file])
        info = parse_badging(badging)
        apk: dict = {
            "hash": common.sha256sum(apk_file),
            "hashType": "sha256",
            "uses-permission": [],
            "uses-permission-sdk-23": [],
            "features": [],
            "icons_src": extract_icons_src(Path(apk_file)),
            "icons": {},
            "antiFeatures": {},
            "packageName": info["packageName"],
            "versionCode": info["versionCode"],
            "versionName": info["versionName"],
            "name": info["name"],
            "minSdkVersion": info["minSdkVersion"],
            "targetSdkVersion": info["targetSdkVersion"],
        }
        apk["sig"] = getsig(apk_file)
        if require_signature and not apk["sig"]:
            from fdroidserver.exception import BuildException

            raise BuildException("Failed to get APK signing key fingerprint")
        apk["signer"] = common.apk_signer_fingerprint(apk_file)
        apk["size"] = os.path.getsize(apk_file)
        return apk

    import fdroidserver.update as update

    update.scan_apk = scan_apk_aapt
    update.scan_apk_androguard = lambda apk, apkfile: None  # unused



def install_fallback_icons(apks: list, repo_dir: str, icon_path: Path) -> None:
    """Copy bundled PNG icons when the APK has only adaptive vector icons."""
    from fdroidserver.update import dpi_to_px, get_icon_dir, screen_densities
    from PIL import Image

    if not icon_path.is_file():
        return

    im = Image.open(icon_path)
    for apk in apks:
        if apk.get("icons"):
            continue
        iconfilename = f"{apk['packageName']}.{apk['versionCode']}.png"
        apk["icons"] = {}
        for density in screen_densities:
            size = dpi_to_px(density)
            icon_dir = get_icon_dir(repo_dir, density)
            Path(icon_dir).mkdir(parents=True, exist_ok=True)
            dest = Path(icon_dir) / iconfilename
            thumb = im.copy()
            thumb.thumbnail((size, size), Image.LANCZOS)
            thumb.save(dest, "PNG", optimize=True)
            apk["icons"][density] = iconfilename
        apk["icon"] = iconfilename

def main() -> None:
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <apk-path> <fdroid-dir>", file=sys.stderr)
        sys.exit(1)

    apk_src = Path(sys.argv[1]).resolve()
    fdroid_dir = Path(sys.argv[2]).resolve()
    repo_dir = fdroid_dir / "repo"
    metadata_dir = fdroid_dir / "metadata"

    if not apk_src.is_file():
        raise SystemExit(f"APK not found: {apk_src}")

    repo_dir.mkdir(parents=True, exist_ok=True)
    metadata_dir.mkdir(parents=True, exist_ok=True)

    # Ensure metadata exists for the package.
    meta_file = metadata_dir / "de.openkleinanzeigen.yml"
    root_meta = fdroid_dir.parent / "metadata" / "de.openkleinanzeigen.yml"
    if root_meta.exists() and not meta_file.exists():
        shutil.copy2(root_meta, meta_file)
    if not meta_file.exists():
        raise SystemExit(f"Missing {meta_file}")

    patch_scan_apk_aapt_only()

    os.chdir(fdroid_dir)
    from argparse import ArgumentParser

    from fdroidserver import common, metadata
    from fdroidserver.index import make as index_make
    from fdroidserver import update as fdroid_update

    parser = ArgumentParser()
    common.setup_global_opts(parser)
    parser.add_argument("--create-key", action="store_true", default=False)
    parser.add_argument("-c", "--create-metadata", action="store_true", default=False)
    parser.add_argument("--delete-unknown", action="store_true", default=False)
    parser.add_argument("-I", "--icons", action="store_true", default=False)
    parser.add_argument("--pretty", action="store_true", default=False)
    parser.add_argument("--clean", action="store_true", default=False)
    parser.add_argument("--nosign", action="store_true", default=False)
    parser.add_argument("--use-date-from-apk", action="store_true", default=False)
    parser.add_argument("--rename-apks", action="store_true", default=False)
    parser.add_argument(
        "--allow-disabled-algorithms",
        action="store_true",
        default=False,
    )
    metadata.add_metadata_arguments(parser)
    sys.argv = ["fdroid update", "--clean"]
    common.options = common.parse_args(parser)
    metadata.warnings_action = common.options.W

    common.config = common.read_config()
    common.fill_config_defaults(common.config)
    fdroid_update.config = common.config
    fdroid_update.options = common.options
    common.setup_status_output(time.gmtime())

    if not (("jarsigner" in common.config or "apksigner" in common.config) and "keytool" in common.config):
        raise SystemExit("Java JDK not found (jarsigner/keytool required for signed index)")

    info = parse_badging(run([find_aapt(), "dump", "badging", str(apk_src)]))
    dest_name = f"{info['packageName']}_{info['versionCode']}.apk"
    shutil.copy2(apk_src, repo_dir / dest_name)

    apps = metadata.read_metadata()
    if info["packageName"] not in apps:
        raise SystemExit(f"No metadata for {info['packageName']}")

    apkcache = fdroid_update.get_cache()
    cache_ts = fdroid_update.get_cache_mtime()
    apks, cachechanged = fdroid_update.process_apks(
        apkcache,
        "repo",
        common.KnownApks(),
        common.options.use_date_from_apk,
        apps,
        cache_ts,
    )
    if not apks:
        raise SystemExit("fdroid process_apks produced no APK entries")

    install_fallback_icons(apks, "repo", fdroid_dir.parent / "metadata" / "app-icon.png")
    fdroid_update.read_added_date_from_all_apks(apps, apks)
    if cachechanged:
        fdroid_update.write_cache(apkcache)

    repoapps = fdroid_update.prepare_apps(apps, apks, "repo")
    index_make(repoapps, apks, "repo", archive=False)

    required = [
        repo_dir / "index.html",
        repo_dir / "index.png",
        repo_dir / "index-v1.json",
        repo_dir / "index-v1.jar",
        repo_dir / "index.css",
    ]
    missing = [p for p in required if not p.exists()]
    if missing:
        raise SystemExit("Missing repo files: " + ", ".join(str(p) for p in missing))

    icons = list(repo_dir.glob(f"icons-*/*{info['packageName']}.*.png"))
    if not icons:
        print("WARNING: no per-app icon PNG in repo/icons-*", file=sys.stderr)

    print(f"F-Droid repo OK ({len(apks)} apk(s), index.html + QR present)")


if __name__ == "__main__":
    main()
