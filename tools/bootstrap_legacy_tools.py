"""Prepare the exact legacy remapping tool before compiling pinned dependencies."""

import argparse
import hashlib
import io
import os
from pathlib import Path
import tempfile
import urllib.error
import urllib.request
import zipfile


SHA256 = "9105207CC68FBBA768B314FA8584F7C63DE2CF664F1FD50AAAB4A386975A88BD"
SHA1 = "a70987737b33a1d828713fb7cec4622455618fd9"
ARTIFACT = "net/md-5/SpecialSource/1.8.3/SpecialSource-1.8.3-shaded.jar"
MIRRORS = ("https://repo.maven.apache.org/maven2/", "https://repo1.maven.org/maven2/")


def valid(content):
    if hashlib.sha256(content).hexdigest().upper() != SHA256:
        raise ValueError("The legacy remapping tool does not match its pinned SHA-256")
    with zipfile.ZipFile(io.BytesIO(content)) as archive:
        manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
        if "Main-Class: net.md_5.specialsource.SpecialSource" not in manifest:
            raise ValueError("The legacy remapping tool has an unexpected entry point")


def prepare(cache_root):
    target = cache_root / "caches/maven/mcp-tools" / ARTIFACT
    if target.is_file():
        content = target.read_bytes()
        if hashlib.sha256(content).hexdigest().upper() == SHA256:
            valid(content)
            Path(str(target) + ".sha1").write_text(SHA1, encoding="utf-8")
            print("LEGACY_TOOL_VERIFIED existing cache")
            return

    failures = []
    for mirror in MIRRORS:
        try:
            with urllib.request.urlopen(mirror + ARTIFACT, timeout=30) as response:
                content = response.read()
        except (urllib.error.URLError, TimeoutError) as failure:
            failures.append(str(failure))
            continue
        # A successful HTTP response must contain the exact tool. Do not accept HTML or a different jar.
        valid(content)
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.is_file():
            rejected = target.read_bytes()
            retained = cache_root / "rejected" / (hashlib.sha256(rejected).hexdigest() + ".jar")
            retained.parent.mkdir(parents=True, exist_ok=True)
            if not retained.exists():
                retained.write_bytes(rejected)
        with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as temporary:
            temporary.write(content)
            staged = Path(temporary.name)
        try:
            os.replace(staged, target)
        finally:
            staged.unlink(missing_ok=True)
        Path(str(target) + ".sha1").write_text(SHA1, encoding="utf-8")
        print("LEGACY_TOOL_VERIFIED " + mirror)
        return
    raise RuntimeError("Could not download the pinned legacy remapping tool: " + "; ".join(failures))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    default = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
    parser.add_argument("--cache-root", type=Path,
                        default=default / "caches/minecraftforge/forgegradle/mavenizer")
    prepare(parser.parse_args().cache_root)
