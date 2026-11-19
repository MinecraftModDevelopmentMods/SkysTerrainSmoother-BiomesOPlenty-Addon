"""Check the dependency commands used when building and publishing a release."""
from pathlib import Path
import os
import re
import shutil
import subprocess
import tempfile
import textwrap
import unittest


class ReleaseDependenciesTest(unittest.TestCase):
    def test_release_and_maven_steps_verify_the_current_bop_dependency(self):
        workflow = Path(".github/workflows/deploy-release.yml").read_text()
        scripts = re.findall(r"- name: Build development dependencies\n        run: \|\n"
                             r"((?:          [^\n]*\n|\n)+)", workflow)
        self.assertEqual(len(scripts), 2, "Both artifact building and Maven publication need the content jars")
        bash = Path(r"C:\Program Files\Git\bin\bash.exe") if os.name == "nt" else shutil.which("bash")
        for index, script in enumerate(scripts):
            with self.subTest(step=index):
                with tempfile.TemporaryDirectory() as temporary:
                    root = Path(temporary)
                    capture = root / "calls"
                    for name in ["grass", "pieces", "terrain", "bopPieces"]:
                        directory = root / "dependencies" / name
                        directory.mkdir(parents=True)
                        wrapper = directory / "gradlew"
                        wrapper.write_text('#!/bin/bash\nprintf "%s|%s\\n" "${PWD##*/}" "$*" >> "$CAPTURE"\n')
                        wrapper.chmod(0o755)
                    subprocess.run([str(bash), "-c", textwrap.dedent(script)], cwd=root,
                                   env=dict(os.environ, CAPTURE=capture.as_posix()), check=True, capture_output=True)
                    calls = dict(line.split("|", 1) for line in capture.read_text().splitlines())
                self.assertEqual(set(calls), {"grass", "pieces", "terrain", "bopPieces"})
                self.assertIn("verifyNativeBopDependencies", calls["bopPieces"].split())
                self.assertIn("-PbuildingPiecesCoreDir=../pieces", calls["bopPieces"].split())
