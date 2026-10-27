#!/usr/bin/env python3
"""Baut ein eigenständiges Firmenpaket ohne Registry und ohne Zugangsdaten."""

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tarfile
from pathlib import Path


def run(*args, **kwargs):
    subprocess.run(args, check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", help="Feste Releaseversion, z. B. 1.0.0")
    parser.add_argument("--arch", choices=["amd64", "arm64"], default="amd64")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?", args.version):
        parser.error("Eine Version wie 1.0.0 oder 1.0.0-rc.1 angeben.")

    root = Path(__file__).resolve().parent.parent
    release = root / "release"
    name = f"deskvision-holter-{args.version}-{args.arch}"
    target = release / name
    archive = release / f"{name}.tar.gz"
    if target.exists() or archive.exists():
        parser.error(f"{name} existiert bereits. Neue Version oder leeres Ziel verwenden.")
    run("docker", "info", "--format", "{{.OSType}}/{{.Architecture}}")
    images = []
    for service, context in [("backend", "Backend"), ("frontend", "Frontend")]:
        tag = f"deskvision/{service}:{args.version}-{args.arch}"
        run("docker", "buildx", "build", "--platform", f"linux/{args.arch}",
            "--load", "--tag", tag, str(root / context))
        images.append(tag)
    mongo = f"deskvision/mongodb:7-{args.arch}"
    run("docker", "pull", "--platform", f"linux/{args.arch}", "mongo:7")
    run("docker", "tag", "mongo:7", mongo)
    images.append(mongo)

    target.mkdir(parents=True)
    # Explizite Dateiliste: keine .env-Dateien, Secrets oder Backups verpacken.
    for filename in ["compose.yml", ".env.example", "README.md", "setup.js", "mongo-init.js"]:
        shutil.copyfile(root / "deployment" / "holter" / filename, target / filename)
    env = (target / ".env.example").read_text()
    env = re.sub(r"^DESKVISION_VERSION=.*$", f"DESKVISION_VERSION={args.version}", env, flags=re.M)
    env = re.sub(r"^DESKVISION_ARCH=.*$", f"DESKVISION_ARCH={args.arch}", env, flags=re.M)
    (target / ".env.example").write_text(env)
    run("docker", "image", "save", "--platform", f"linux/{args.arch}",
        "--output", str(target / "images.tar"), *images)
    metadata = json.loads(subprocess.check_output(
        ["docker", "image", "inspect", "--platform", f"linux/{args.arch}", *images], text=True))
    manifest = {
        "version": args.version,
        "platform": f"linux/{args.arch}",
        "images": [{"tags": item["RepoTags"], "id": item["Id"], "architecture": item["Architecture"]}
                   for item in metadata],
    }
    (target / "release.json").write_text(json.dumps(manifest, indent=2) + "\n")
    hashes = []
    for file in sorted(target.iterdir()):
        digest = hashlib.sha256()
        with file.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        hashes.append(f"{digest.hexdigest()}  {file.name}\n")
    (target / "SHA256SUMS").write_text("".join(hashes))
    with tarfile.open(archive, "w:gz", compresslevel=1) as bundle:
        bundle.add(target, arcname=name)
    digest = hashlib.sha256()
    with archive.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    Path(str(archive) + ".sha256").write_text(f"{digest.hexdigest()}  {archive.name}\n")
    # Die entpackte Arbeitskopie bleibt für die Abnahme und lokale Tests erhalten.
    print(f"Firmenpaket: {archive}", flush=True)
    print(f"SHA-256: {digest.hexdigest()}", flush=True)


if __name__ == "__main__":
    main()
