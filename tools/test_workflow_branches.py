"""Make sure wrapper validation follows the branch's Minecraft and loader."""
from pathlib import Path
import fnmatch
import unittest


class WorkflowBranchesTest(unittest.TestCase):
    def test_wrapper_validation_runs_for_this_release_branch(self):
        properties = dict(line.split("=", 1) for line in Path("gradle.properties").read_text().splitlines()
                          if "=" in line and not line.startswith("#"))
        branch = "master-" + properties["minecraft_version"]
        if properties["loader_code"] == "2":
            branch += "-neo"
        lines = Path(".github/workflows/validate-gradle-build.yml").read_text().splitlines()
        for event in ["push", "pull_request"]:
            with self.subTest(event=event, branch=branch):
                start = lines.index("  " + event + ":") + 1
                section = []
                for line in lines[start:]:
                    if line.strip() and not line.startswith("    "):
                        break
                    section.append(line)
                definition = next(line for line in section if line.strip().startswith("branches:"))
                value = definition.split(":", 1)[1].strip()
                if value.startswith("["):
                    patterns = value[1:-1].split(",")
                else:
                    patterns = [line.strip()[2:] for line in section if line.strip().startswith("- ")]
                patterns = [value.strip().strip("'\"") for value in patterns]
                self.assertTrue(any(fnmatch.fnmatchcase(branch, pattern) for pattern in patterns),
                                "Wrapper validation does not cover " + branch)
