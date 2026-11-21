"""Keep the native forward-upgrade reference tied to its qualified source."""
from pathlib import Path
import hashlib
import json
import unittest

FIXTURES = Path(__file__).resolve().parents[1] / "src/test/resources/fixtures"

class NativeReferenceTest(unittest.TestCase):
    def test_canonical_capture_is_complete_and_sealed(self):
        contract = json.loads((FIXTURES / "canonical-26.1.2-contract.json").read_text())
        raw = (FIXTURES / contract["file"]).read_bytes()
        self.assertEqual(contract["sha256"], hashlib.sha256(raw).hexdigest().upper())
        states = json.loads(raw)
        self.assertEqual(4700, len(states))
        self.assertEqual(4700, len({(s["x"], s["y"], s["z"]) for s in states}))
        self.assertEqual("forge-64.0.9", contract["reference_loader"])
        self.assertEqual(5, len(contract["reference_artifacts"]))
        self.assertEqual(5, len(contract["validated_artifacts"]))
        self.assertEqual(2, len(set(contract["source_native_sha256"])))
        self.assertIn(contract["sha256"], contract["source_native_sha256"])

if __name__ == "__main__":
    unittest.main()
