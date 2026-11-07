"""Run a test-only probe in a disposable NeoGradle client or server."""

import argparse
import json
import os
from pathlib import Path
import subprocess

from run_packaged_checks import audit, digest, prepare


# The exact vendor jars used to reproduce the allay notice on a development
# server with no Sky mods. A different vendor build still gets the strict audit.
BOP_SPAWN_CONTROL_HASHES = frozenset({
    "5F3C8D752C2E0464ECA8C9527D8FFE498129FD37A2FE86A266B64C98371763B1",
    "0C49B5EF447A7F09100E9A210888A7347FBDA0AA75322063991F6063B25FE3F9",
    "59D2A3FB3D6877E43018FD6A2B199D526074A864276A5F49091409A1CDC62FAE",
})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--probe", type=Path, required=True)
    parser.add_argument("--mode", choices=("server", "client"), required=True)
    parser.add_argument("--phase", required=True)
    parser.add_argument("--extra-mod", type=Path, action="append", default=[])
    parser.add_argument("--fresh", action="store_true")
    parser.add_argument("--gradle-argument", action="append", default=[])
    options = parser.parse_args()
    if not all(part.isalnum() for part in options.phase.split("-")):
        raise ValueError("Use a simple probe phase name")
    directory = options.directory.resolve()
    build = Path("build").resolve()
    if not directory.is_relative_to(build) or directory == build:
        raise ValueError("Development checks need a disposable directory under build")
    options.mod = options.fixture = None
    options.level = options.enabled = options.grass_enabled = None
    options.bop_world = False
    if options.fresh:
        prepare(options, directory)
    expected = [{"name": path.name, "sha256": digest(path)}
                for path in (options.probe, *options.extra_mod)]
    if json.loads((directory / "artifact-inputs.json").read_text(encoding="utf-8")) != expected:
        raise ValueError("Probe or companion inputs changed between loads")
    for artifact in expected:
        if digest(directory / "mods" / artifact["name"]) != artifact["sha256"]:
            raise ValueError("A copied probe input changed: " + artifact["name"])

    wrapper = Path("gradlew.bat" if os.name == "nt" else "gradlew").resolve()
    command = [str(wrapper), "run" + options.mode.capitalize(), "--no-daemon", "--console=plain",
               "--max-workers=2", "-PskysTerrainSmootherBopRunDirectory=" + directory.relative_to(Path.cwd()).as_posix(),
               "-PskysTerrainSmootherBopProbePhase=" + options.phase, *options.gradle_argument]
    console = directory / (options.phase + "-console.log")
    with console.open("x", encoding="utf-8") as output:
        subprocess.run(command, check=True, stdout=output, stderr=subprocess.STDOUT, timeout=600)
    vendor_inputs = {row["sha256"] for row in expected}
    bop_development = options.mode == "server" and BOP_SPAWN_CONTROL_HASHES <= vendor_inputs
    audit(directory, options.phase, options.mode, bop_development=bop_development)
    print("NEOFORGE_TERRAIN_BOP_DEVELOPMENT_PASS", options.mode, options.phase)


if __name__ == "__main__":
    main()
