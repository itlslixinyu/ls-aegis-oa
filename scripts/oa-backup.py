"""Online Btrfs snapshot backup and recovery of the fnOS OA deployment."""
import argparse
import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


ROOT = Path("/vol1/ls-aegis-oa")
CONF = ROOT / "compose.yaml"
STORE = Path("/vol1/ls-aegis-backups")
PROJECT = "ls-aegis-oa-service"
SERVICES = {"ls-aegis-" + s for s in
            ("mysql", "redis", "server", "web", "h5", "h5-engineer", "mobile-gateway")}
STAMP = r"[0-9]{8}T[0-9]{6}Z"


def run(*args, output=False):
    return subprocess.run(args, check=True, text=True, capture_output=output).stdout


def dc(*args, output=False):
    return run("docker", "compose", "--project-directory", ROOT, "-p", PROJECT,
               "-f", CONF, *args, output=output)


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def timestamp():
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def inventory():
    model = json.loads(dc("config", "--format", "json", output=True))
    if set(model["services"]) != SERVICES:
        raise ValueError("Unexpected service list")
    ids = dc("ps", "-a", "-q", output=True).split()
    if len(ids) != len(SERVICES):
        raise ValueError("Expected seven containers")
    rows = json.loads(run("docker", "inspect", *ids, output=True))
    paths, images = set(), {}
    for row in rows:
        name = row["Name"].lstrip("/")
        service = model["services"][name]
        state = row["State"]
        if state["Status"] != "running" or state.get("Health", {}).get("Status") not in (None, "healthy"):
            raise ValueError("Service not ready: " + name)
        images[name] = row["Image"]
        actual = {m["Destination"]: (m["Type"], Path(m["Source"]).resolve())
                  for m in row["Mounts"]}
        declared = {}
        for mount in service.get("volumes", []):
            path = Path(mount["source"]).resolve(strict=True)
            path.relative_to(ROOT)
            if mount["type"] != "bind" or path == ROOT:
                raise ValueError("Unexpected mount: " + str(path))
            declared[mount["target"]] = ("bind", path)
            mount["source"] = str(path)
            paths.add(path)
        if actual != declared:
            raise ValueError("Live mounts differ from Compose: " + name)
    # Capture the active configuration only, excluding prior installations beside it.
    roots = sorted(p for p in paths if not any(q in p.parents for q in paths))
    original = json.loads(CONF.read_text())
    for name, service in original["services"].items():
        if "volumes" in service:
            service["volumes"] = model["services"][name]["volumes"]
    return original, images, roots


def backup(*, test_restore=False):
    model, images, roots = inventory()
    name = timestamp()
    with tempfile.TemporaryDirectory(dir=STORE, prefix=".pending-") as temp:
        stage = Path(temp)
        size = sum(int(run("du", "-sb", p, output=True).split()[0]) for p in roots)
        if shutil.disk_usage(STORE).free < size * 1.05 + 256 * 1024**2:
            raise ValueError("Insufficient backup space")
        archive = stage / "data.tar"
        archive_snapshot(roots, archive)
        with open(os.devnull, "wb") as sink:
            subprocess.run(["tar", "-tf", archive], check=True, stdout=sink)
        (stage / "model.json").write_text(json.dumps(model))
        (stage / "images.json").write_text(json.dumps(images))
        files = ("data.tar", "model.json", "images.json")
        (stage / "checks.json").write_text(json.dumps({f: digest(stage / f) for f in files}))
        if test_restore:
            drill(stage)
        stage.rename(STORE / name)
    rotate(name)
    print("BACKUP_READY", name, STORE / name, flush=True)
    if test_restore:
        print("RESTORE_DRILL_OK", name, flush=True)
    return name


def beijing_date(name):
    utc = datetime.datetime.strptime(name, "%Y%m%dT%H%M%SZ")
    return (utc + datetime.timedelta(hours=8)).date()


def rotate(latest):
    today = beijing_date(latest)
    kept = set()
    for copy in sorted(STORE.iterdir(), reverse=True):
        if (not re.fullmatch(STAMP, copy.name) or copy.is_symlink() or not copy.is_dir()
                or not (copy / "checks.json").is_file() or copy.name > latest):
            continue
        day = beijing_date(copy.name)
        if (today - day).days >= 7 or day in kept:
            shutil.rmtree(copy)
        else:
            kept.add(day)


def archive_snapshot(roots, archive):
    volume = Path("/vol1")
    if run("findmnt", "-n", "-o", "FSTYPE", "-T", ROOT, output=True).strip() != "btrfs":
        raise ValueError("Online backup requires Btrfs")
    subvol = ROOT
    while subvol.stat().st_ino != 256:
        if subvol == volume:
            raise ValueError("Unable to locate a snapshot-capable subvolume")
        subvol = subvol.parent
    # A mounted filesystem below a data root would be invisible in its snapshot.
    for line in Path("/proc/self/mountinfo").read_text().splitlines():
        mount = Path(re.sub(r"\\([0-7]{3})", lambda m: chr(int(m[1], 8)), line.split()[4]))
        if (mount != subvol and subvol in mount.parents and
                any(mount == p or mount in p.parents or p in mount.parents for p in roots)):
            raise ValueError("Separate data mount: " + str(mount))
    for path in roots:
        if path.stat().st_dev != subvol.stat().st_dev:
            raise ValueError("Data spans multiple subvolumes: " + str(path))
    # Avoid TemporaryDirectory recursive cleanup of a read-only subvolume on errors.
    private = Path(tempfile.mkdtemp(dir=volume, prefix=".oa-snapshot-"))
    snap = private / "snapshot"
    try:
        run("btrfs", "subvolume", "snapshot", "-r", subvol, snap)
        base = snap / ROOT.relative_to(subvol)
        relative = [p.relative_to(ROOT) for p in roots]
        for p in relative:
            copy = (base / p).resolve(strict=True)
            copy.relative_to(snap)
            if run("find", copy, "-inum", "2", "-print", "-quit", output=True).strip():
                raise ValueError("Nested subvolume omitted from snapshot: " + str(p))
        run("tar", "--acls", "--xattrs", "--sparse", "-cf", archive, "-C", base, "--", *relative)
    finally:
        if snap.exists():
            run("btrfs", "subvolume", "delete", snap)
        private.rmdir()


def recover(name, activate):
    if not re.fullmatch(STAMP, name):
        raise ValueError("Specify an exact backup timestamp")
    src = STORE / name
    checks = json.loads((src / "checks.json").read_text())
    for filename in ("data.tar", "model.json", "images.json"):
        if digest(src / filename) != checks[filename]:
            raise ValueError("Checksum mismatch: " + filename)
    if not activate:
        drill(src)
        print("RESTORE_DRILL_OK", name)
        return
    model = json.loads((src / "model.json").read_text())
    images = json.loads((src / "images.json").read_text())
    if set(model["services"]) != SERVICES or set(images) != SERVICES:
        raise ValueError("Unexpected backup service list")
    for svc, settings in model["services"].items():
        if run("docker", "image", "inspect", "-f", "{{.Id}}", settings["image"],
               output=True).strip() != images[svc]:
            raise ValueError("Matching Docker image required: " + svc)
    target = Path(tempfile.mkdtemp(prefix="recovered-", dir=ROOT))
    run("tar", "--acls", "--xattrs", "--numeric-owner", "-xf", src / "data.tar", "-C", target)
    for settings in model["services"].values():
        for m in settings.get("volumes", []):
            path = target / Path(m["source"]).relative_to(ROOT)
            m["source"] = str(path.resolve(strict=True))
    saved = target / "prior-compose.yaml"
    shutil.copy2(CONF, saved)
    pending = target / "recovered-compose.json"
    pending.write_text(json.dumps(model))
    run("docker", "compose", "--project-directory", ROOT, "-p", PROJECT,
        "-f", pending, "config", "--quiet")
    try:
        dc("stop", "-t", "120")
        os.replace(pending, CONF)
        dc("up", "-d", "--force-recreate", "--wait", "--wait-timeout", "300")
        inventory()
    except BaseException:
        shutil.copy2(saved, CONF)
        dc("up", "-d", "--force-recreate", "--wait", "--wait-timeout", "300")
        raise
    print("RESTORE_READY", name, "previous configuration:", saved)


def drill(src):
    model = json.loads((src / "model.json").read_text())
    db = model["services"]["ls-aegis-mysql"]
    image = db["image"]
    if run("docker", "image", "inspect", "-f", "{{.Id}}", image, output=True).strip() != json.loads(
            (src / "images.json").read_text())["ls-aegis-mysql"]:
        raise ValueError("Local MySQL image differs from backup")
    with tempfile.TemporaryDirectory(dir=STORE, prefix=".drill-") as temp:
        dest = Path(temp)
        volumes = db["volumes"]
        # Unpack into a private directory; never mount production data in a drill.
        run("tar", "--numeric-owner", "-xf", src / "data.tar", "-C", dest)
        for mount in volumes:
            mount["source"] = str((dest / Path(mount["source"]).relative_to(ROOT)).resolve(strict=True))
        for key in ("container_name", "networks", "ports", "depends_on"):
            db.pop(key, None)
        db.update(network_mode="none", restart="no")
        file = dest / "drill.json"
        file.write_text(json.dumps({"services": {"db": db}}))
        cmd = ["docker", "compose", "-p", "oa-drill-" + dest.name.split("-")[-1].replace("_", "x"),
               "-f", file]
        try:
            run(*cmd, "up", "-d", "--wait", "--wait-timeout", "300")
            run(*cmd, "exec", "-T", "db", "sh", "-ec",
                'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot ls_aegis -e '
                '"SELECT COUNT(*) AS users_total FROM sys_user; SELECT COUNT(*) AS orders_total FROM eoms_repair_order"')
        finally:
            run(*cmd, "down", "-t", "120")


def install():
    script = Path("/usr/local/sbin/ls-aegis-backup.py")
    if Path(__file__).resolve() != script:
        shutil.copyfile(__file__, script)
    script.chmod(0o700)
    units = Path("/etc/systemd/system")
    (units / "ls-aegis-backup.service").write_text(
        "[Unit]\nDescription=Back up LS Aegis OA\nAfter=docker.service\n"
        "[Service]\nType=oneshot\nTimeoutStartSec=infinity\nUMask=0077\n"
        f"ExecStart=/usr/bin/python3 {script} backup\n")
    (units / "ls-aegis-backup.timer").write_text(
        "[Unit]\nDescription=Nightly LS Aegis OA backup\n[Timer]\n"
        "OnCalendar=*-*-* 02:00:00 Asia/Shanghai\nAccuracySec=1s\nPersistent=false\n"
        "[Install]\nWantedBy=timers.target\n")
    run("systemctl", "daemon-reload")
    run("systemctl", "enable", "--now", "ls-aegis-backup.timer")
    run("systemctl", "list-timers", "ls-aegis-backup.timer")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("install", "backup", "verify", "restore"))
    parser.add_argument("timestamp", nargs="?")
    args = parser.parse_args()
    os.umask(0o077)
    if os.geteuid() or not os.path.ismount("/vol1"):
        raise SystemExit("Requires root and mounted /vol1")
    STORE.mkdir(mode=0o700, exist_ok=True)
    if STORE.is_symlink() or STORE.stat().st_uid != 0 or STORE.stat().st_mode & 0o077:
        raise SystemExit("Backup directory must be private to root")
    with (STORE / ".lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if args.command == "install":
            backup(test_restore=True)
            install()
        elif args.command == "backup":
            backup()
        else:
            recover(args.timestamp or "", args.command == "restore")
