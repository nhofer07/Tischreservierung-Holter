#!/usr/bin/env python3
"""Prüft das Firmenpaket in einem isolierten Compose-Projekt mit Wegwerfdaten."""

import argparse
import json
import os
import shutil
import subprocess
import tempfile
import uuid
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path, help="Entpackter Paketordner")
    parser.add_argument("--port", type=int, default=13000)
    args = parser.parse_args()
    package = args.package.resolve()
    manifest = json.loads((package / "release.json").read_text())
    arch = manifest["platform"].split("/")[1]
    project = "deskvision-check-" + uuid.uuid4().hex[:10]

    def run(*command, **options):
        return subprocess.run(command, check=True, **options)

    run("docker", "load", "--input", str(package / "images.tar"))
    with tempfile.TemporaryDirectory(prefix="company-check-", dir=package.parent) as temporary:
        work = Path(temporary)
        for filename in ["compose.yml", ".env.example", "setup.js", "mongo-init.js"]:
            shutil.copyfile(package / filename, work / filename)
        setup = ["docker", "run", "--rm", "--network", "none", "--platform", manifest["platform"],
                 "--env", "HOME=/tmp", "--env", "BOOTSTRAP_ADMIN_EMAIL=admin@example.test",
                 "--mount", f"type=bind,source={work},target=/handoff", "--workdir", "/handoff"]
        if hasattr(os, "getuid"):
            setup += ["--user", f"{os.getuid()}:{os.getgid()}"]
        run(*setup, "--entrypoint", "mongosh", f"deskvision/mongodb:7-{arch}",
            "--nodb", "--quiet", "--file", "setup.js")
        env = (work / ".env").read_text().replace("DESKVISION_PORT=3000", f"DESKVISION_PORT={args.port}")
        (work / ".env").write_text(env)
        compose = ["docker", "compose", "--project-name", project, "--project-directory", str(work)]

        def mongo(js):
            auth = "const d=db.getSiblingDB('tischreservierung');" \
                   "d.auth('deskvision-app',require('fs').readFileSync('/run/secrets/mongo_app_password','utf8').trim());"
            run(*compose, "exec", "-T", "mongodb", "mongosh", "--quiet", "--eval", auth + js)

        try:
            run(*compose, "up", "-d", "--wait", "--wait-timeout", "300")
            base = f"http://127.0.0.1:{args.port}"
            with urlopen(base, timeout=10) as response:
                assert b"<app-root>" in response.read()
            with urlopen(base + "/runtime-config.js", timeout=10) as response:
                config = response.read().decode()
                assert '"enableLocalTestLogin":false' in config
                assert response.headers.get("Cache-Control") == "no-store"
            with urlopen(base + "/api/einstellungen", timeout=10) as response:
                assert json.load(response)["firmenname"] == "HOLTER"
            try:
                urlopen(base + "/api/benutzer", timeout=10)
                raise AssertionError("Geschützte API war ohne Microsoft-Token erreichbar")
            except HTTPError as error:
                assert error.code == 401
            mongo("if(d.benutzer.countDocuments()!==1||d.reservierungen.countDocuments()!==0)throw Error('Testdaten gefunden');"
                  "const a=d.benutzer.findOne({_id:'SUPERADMIN-HOLTER'});"
                  "if(a.email!=='admin@example.test'||a.rolle!=='SUPERADMIN'||a.passwortHash)throw Error('Admin falsch');"
                  "d.einstellungen.insertOne({_id:'BACKUP-CHECK',value:'vorher'});")
            run(*compose, "stop", "frontend", "backend")
            dump = 'mongodump --host 127.0.0.1 --username "$MONGO_INITDB_ROOT_USERNAME" ' \
                   '--password "$(cat /run/secrets/mongo_root_password)" --authenticationDatabase admin ' \
                   '--db tischreservierung --archive=/tmp/deskvision-backup.archive.gz --gzip'
            run(*compose, "exec", "-T", "mongodb", "sh", "-ec", dump)
            backup = work / "deskvision-backup.archive.gz"
            run(*compose, "cp", "mongodb:/tmp/deskvision-backup.archive.gz", str(backup))
            assert backup.stat().st_size > 0
            mongo("d.einstellungen.updateOne({_id:'BACKUP-CHECK'},{$set:{value:'nachher'}});")
            run(*compose, "cp", str(backup), "mongodb:/tmp/deskvision-restore.archive.gz")
            restore = 'mongorestore --host 127.0.0.1 --username "$MONGO_INITDB_ROOT_USERNAME" ' \
                      '--password "$(cat /run/secrets/mongo_root_password)" --authenticationDatabase admin ' \
                      '--archive=/tmp/deskvision-restore.archive.gz --gzip --nsInclude="tischreservierung.*" --drop'
            run(*compose, "exec", "-T", "mongodb", "sh", "-ec", restore)
            mongo("if(d.einstellungen.findOne({_id:'BACKUP-CHECK'}).value!=='vorher')throw Error('Restore fehlgeschlagen');")
            run(*compose, "down")
            run(*compose, "up", "-d", "--wait", "--wait-timeout", "300")
            mongo("if(d.benutzer.countDocuments()!==1||d.einstellungen.findOne({_id:'BACKUP-CHECK'}).value!=='vorher')throw Error('Persistenz fehlgeschlagen');")
            print("Firmenpaket geprüft: Image-Import, Start, API-Schutz, Admin, Backup, Restore und Persistenz.", flush=True)
        except Exception:
            subprocess.run([*compose, "logs", "--tail", "60"])
            raise
        finally:
            # Ausschließlich das oben zufällig benannte Testprojekt und sein Testvolume.
            run(*compose, "down", "--volumes")


if __name__ == "__main__":
    main()
