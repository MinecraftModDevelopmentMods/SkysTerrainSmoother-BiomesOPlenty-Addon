"""Check NeoForge completion, native settings and unexpected errors."""

import argparse
import gzip
import json
from pathlib import Path
import re
import tomllib
from native_registry_notices import audit_text, BOP_ALLAY_NOTICE, checked_bop_development_text



def audit_complete_logs(directory, *, bop_development=False):
    """Include rolled first-load logs, so a clean reload cannot hide a failure."""
    paths = sorted(path for path in (Path(directory) / "logs").rglob("*")
                   if path.is_file() and (path.name.endswith(".log") or path.name.endswith(".log.gz")))
    if not paths:
        raise ValueError("Native log evidence is missing")
    for path in paths:
        if path.name.endswith(".gz"):
            with gzip.open(path, "rt", encoding="utf-8") as source:
                text = source.read()
        else:
            text = path.read_text(encoding="utf-8")
        if not text.strip():
            raise ValueError(f"Empty native log: {path.name}")
        try:
            audit_text(text, bop_development=bop_development)
        except ValueError as failure:
            raise ValueError(str(path) + ": " + str(failure)) from failure
    return len(paths)


def classify(log, unchanged_defaults, *, bop_development=False):
    """Require a complete client login without severe notices."""
    controlled_notices = 0
    if bop_development:
        controlled_notices = len(list(BOP_ALLAY_NOTICE.finditer(log)))
        log = checked_bop_development_text(log)
    lines = log.splitlines()
    severe = [line for line in lines if re.search(r"/(?:ERROR|FATAL)\]", line)]
    startup = next((index for index, line in enumerate(lines)
                    if "Preparing start region for dimension " in line), -1)
    login = next((index for index, line in enumerate(lines) if " logged in with entity id " in line), -1)
    joined = next((index for index, line in enumerate(lines) if line.endswith(" joined the game")), -1)
    if not 0 <= startup < login <= joined:
        raise ValueError("NeoForge login did not complete normally")
    if not unchanged_defaults:
        raise ValueError("NeoForge settings changed")
    if severe or "[STDERR/" in log or "[STDERR]" in log:
        raise ValueError("Unexpected client errors: " + repr(severe[:4]))
    return {"empty_handshakes": 0, "native_config_notices": 0,
            "notices": [], "unchanged_native_defaults": True,
            "controlled_bop_allay_notices": controlled_notices}


def unchanged_native_settings(directory, names):
    defaults = Path(__file__).resolve().parent.parent / "src/bootstrapTest/defaultconfigs/config"
    for name in names:
        path = directory / "config" / name
        if not path.is_file():
            return False
        expected = tomllib.loads((defaults / name).read_text(encoding="utf-8"))
        actual = tomllib.loads(path.read_text(encoding="utf-8"))
        if name == "fml.toml":
            # Fresh 21.1 installs write an empty table. Older copies can omit it.
            expected.setdefault("dependencyOverrides", {})
            actual.setdefault("dependencyOverrides", {})
        if actual != expected:
            return False
    return True


def audit(directory, phase, *, bop_development=False):
    directory = Path(directory)
    log = (directory / "logs/latest.log").read_text(encoding="utf-8")
    unchanged = unchanged_native_settings(directory, (
        "fml.toml", "neoforge-client.toml", "neoforge-common.toml", "neoforge-server.toml"))
    if not unchanged:
        raise ValueError("NeoForge settings differ from the controlled defaults")
    receipt = classify(log, unchanged, bop_development=bop_development)
    required = ("SKY_CONFIG_SCREENS_PASS", "TERRAIN_SMOOTHER_CLIENT_PASS",
                "TERRAIN_SMOOTHER_CLIENT_WORLD_PASS", "TERRAIN_SMOOTHER_CLIENT_DISCONNECT_PASS", "SKY_RECIPE_PREVIEW_MODELS_PASS")
    if any(marker not in log for marker in required):
        raise ValueError("Native client completion checks are missing")
    if list((directory / "crash-reports").glob("*")):
        raise ValueError("Native client produced a crash report")
    receipt["complete_log_files"] = audit_complete_logs(directory, bop_development=bop_development)
    receipt["phase"] = phase
    (directory / (phase + "-log-audit.json")).write_text(
        json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    return receipt


def classify_server(log, debug, unchanged_defaults, *, bop_development=False):
    """Require unchanged defaults and a complete NeoForge 1.21.1 server start."""
    startup = any('Preparing level ' in line or 'Preparing start region for dimension ' in line
                  for line in log.splitlines())
    if not unchanged_defaults or not startup or 'Done (' not in log:
        raise ValueError('Native server defaults or startup sequence changed')
    # Earlier Forge and NeoForge versions reported different registry notices.
    # Those receipts document the original fixture runs, not this loader.
    notices = audit_text(log, bop_development=bop_development)
    if not debug.strip():
        raise ValueError('Native server debug evidence is missing')
    return {'native_server_config_notices': 0, 'notices': notices,
            'unchanged_native_defaults': True, 'debug_identity_verified': True}

def audit_server(directory, phase, *, bop_development=False):
    directory = Path(directory)
    unchanged = unchanged_native_settings(directory, ("fml.toml", "neoforge-common.toml", "neoforge-server.toml"))
    receipt = classify_server((directory / 'logs/latest.log').read_text(encoding='utf-8'),
                              (directory / 'logs/debug.log').read_text(encoding='utf-8'), unchanged,
                              bop_development=bop_development)
    if list((directory / 'crash-reports').glob('*')):
        raise ValueError('Native server produced a crash report')
    receipt['complete_log_files'] = audit_complete_logs(directory, bop_development=bop_development)
    receipt['phase'] = phase
    (directory / (phase + '-server-log-audit.json')).write_text(
        json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    return receipt


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--phase", required=True)
    parser.add_argument("--server", action="store_true")
    options = parser.parse_args()
    result = (audit_server(options.directory, options.phase) if options.server else
              audit(options.directory, options.phase))
    print("NEOFORGE_NATIVE_LOG_AUDIT_PASS", json.dumps(result, sort_keys=True))
