"""Recognise only checksum-bound, unused vanilla registry entries from upgrade fixtures."""

import hashlib
from collections import Counter
import json
from pathlib import Path
import re


def profile_for(directory):
    manifest = Path(directory) / "source-inputs.json"
    if not manifest.is_file():
        return None
    source = json.loads(manifest.read_text(encoding="utf-8"))
    root = Path(__file__).resolve().parent.parent
    policy = json.loads((root / "tools/forge-45-log-profiles.json").read_text(encoding="utf-8"))
    profiles = policy["profiles"]
    profile = profiles.get(source.get("world_sha256"))
    if profile is None:
        return None
    fixture = root / profile["fixture"]
    saved_manifest = fixture.parent / "source-family-fixture.json"
    provenance = json.loads((fixture.parent / "provenance.json").read_text(encoding="utf-8"))
    if (hashlib.sha256(fixture.read_bytes()).hexdigest().upper() != profile["sha256"]
            or hashlib.sha256(saved_manifest.read_bytes()).hexdigest().upper() != profile["manifest_sha256"]
            or source.get("manifest_sha256") != profile["manifest_sha256"]
            or provenance["source_artifacts"] != profile["source_artifacts"]
            or profile.get("non_biome_saved_uses", profile["saved_uses"]) != 0 or profile["inspected_nbt_records"] <= 0):
        raise ValueError("The unused-entry proof does not match the upgrade fixture")
    proof = profile.get("native_biome_contract")
    if proof is not None:
        contract = root / proof["path"]
        data = json.loads(contract.read_text(encoding="utf-8"))
        if (hashlib.sha256(contract.read_bytes()).hexdigest().upper() != proof["sha256"]
                or data["source_world_sha256"] != profile["sha256"]
                or data["checked_cells"] != proof["checked_cells"]
                or data["native_renamed_cells"] != proof["renamed_cells"]
                or profile["saved_uses"] != proof["renamed_cells"]):
            raise ValueError("The complete biome proof does not match this fixture")
    forge45 = profile.get("forge45_registry_proof")
    if forge45 is None:
        raise ValueError("The Forge 45 registry notice proof is missing")
    else:
        path = root / forge45["path"]
        receipt = json.loads(path.read_text(encoding="utf-8"))
        unused = {name for registry, names in profile["mappings"].items()
                  if registry != "minecraft:worldgen/biome" for name in names}
        if (hashlib.sha256(path.read_bytes()).hexdigest().upper() != forge45["sha256"]
                or receipt["target"] != "Forge 45.4.0"
                or receipt["source_world_sha256"] != profile["sha256"]
                or receipt["snapshot_entries"] != profile["mappings"]
                or receipt["unused_non_biome_ids"] != dict.fromkeys(unused, 0)
                or receipt["inspected_nbt_records"] != profile["inspected_nbt_records"]
                or receipt["inspected_string_values"] <= 0
                or proof is None or receipt["native_biome_contract_sha256"] != proof["sha256"]
                or receipt["checked_biome_cells"] != proof["checked_cells"]
                or receipt["native_renamed_cells"] != proof["renamed_cells"]
                or not 0 <= receipt["noticed_biome_cells"] <= proof["checked_cells"]):
            raise ValueError("The Forge 45 registry notice proof does not match the saved fixture")
    if source.get("mode") == "client":
        # Client notice order is qualified separately from the server.
        return None
    return profile


def completed_offline_client(text):
    return all(marker in text for marker in (
        "TERRAIN_SMOOTHER_CLIENT_PASS", "TERRAIN_SMOOTHER_CLIENT_WORLD_PASS",
        "TERRAIN_SMOOTHER_RUNTIME_PASS phase="))


def checked_offline_profile_warnings(text):
    """Recognise the controlled Authlib 3.3.39 gateway failure for our offline profile."""
    pattern = re.compile(
        r"^\[[^\]\n]+\] \[Render thread/WARN\] "
        r"\[com\.mojang\.authlib\.yggdrasil\.YggdrasilMinecraftSessionService/\]: "
        r"Couldn't look up profile properties for com\.mojang\.authlib\.GameProfile@[0-9a-f]+"
        r"\[id=11111111-1111-1111-1111-111111111111,name=TerrainSmootherChecks,"
        r"properties=\{\},legacy=false\]\n(?P<body>.*?)(?=^\[|\Z)", re.M | re.S)
    expected = [
        "com.mojang.authlib.exceptions.AuthenticationUnavailableException: Cannot contact authentication server",
        "Caused by: com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: "
        "Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $",
        "Caused by: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $"
    ]

    def recognise(match):
        lines = [line for line in match["body"].splitlines() if line.strip()]
        exceptions = [line for line in lines if not re.fullmatch(r"\s+(?:at .+\([^\n]*\).+|\.\.\. \d+ more)", line)]
        if exceptions != expected:
            return match[0]
        required = ("authlib-3.3.39.jar", "YggdrasilMinecraftSessionService.fillGameProfile",
                    "YggdrasilAuthenticationService.makeRequest", "com.google.gson.stream.JsonReader.beginObject")
        if not all(fragment in match[0] for fragment in required):
            return match[0]
        return ""

    if len(list(pattern.finditer(text))) > 1:
        return text
    return pattern.sub(recognise, text)


def audit_text(text, profile=None, completed_native_client=False):
    text = text.replace("\r\n", "\n")
    if completed_native_client:
        text = checked_offline_profile_warnings(text)
    observed = {}
    occurrences = Counter()
    expected_reads = profile.get("notice_reads", 1) if profile else 1
    registry_reads = profile.get("registry_reads", {}) if profile else {}
    expected_thread = profile.get("notice_thread", "main") if profile else "main"
    pattern = r"Unidentified mapping from registry (minecraft:[a-z_/]+)\n(.*?)(?:\n\n)"
    for registry, body in re.findall(pattern, text, re.S):
        entries = {}
        for row in body.splitlines():
            if not row.strip():
                continue
            entry = re.fullmatch(r"\s*([a-z0-9_.-]+:[a-z0-9_/.-]+): (\d+)", row)
            if not entry or entry[1] in entries:
                raise ValueError("Malformed or repeated registry entry: " + row)
            entries[entry[1]] = int(entry[2])
        occurrences[registry] += 1
        if occurrences[registry] > registry_reads.get(registry, expected_reads):
            raise ValueError("Repeated native registry notice: " + registry)
        if registry in observed and entries != observed[registry]:
            raise ValueError("Changed native registry entries between reads: " + registry)
        observed[registry] = entries
    if observed and (profile is None or observed != profile["mappings"]):
        raise ValueError("Unexpected missing registry entries: " + repr(observed))
    if observed and any(count != registry_reads.get(registry, expected_reads)
                        for registry, count in occurrences.items()):
        raise ValueError("The native registry read sequence is incomplete")
    headers = {"Unidentified mapping from registry " + registry for registry in observed}
    if observed:
        headers.add("There are unidentified mappings in this world - we are going to attempt to process anyway")
    header_reads = {"Unidentified mapping from registry " + registry:
                    registry_reads.get(registry, expected_reads) for registry in observed}
    seen = Counter()
    for line in text.splitlines():
        if not re.search(r"/(?:ERROR|FATAL)\]|\[STDERR[/\]]", line):
            continue
        message = re.fullmatch(r"\[[^\r\n]+\] \[" + re.escape(expected_thread) + r"/ERROR\] "
                               r"\[net\.minecraftforge\.registries\.GameData/REGISTRIES\]: (.+)", line)
        if message and message[1] in headers and seen[message[1]] < header_reads.get(message[1], expected_reads):
            seen[message[1]] += 1
        else:
            raise ValueError("Unexpected native failure: " + line)
    if set(seen) != headers or any(count != header_reads.get(header, expected_reads)
                                   for header, count in seen.items()):
        raise ValueError("The native registry notice is incomplete")
    for problem in ("Encountered an unexpected exception", "Exception stopping the server",
                    "MissingModsException", "NoSuchMethodError", "NoClassDefFoundError",
                    "ExceptionInInitializerError", "cascading worldgen lag", "Unable to load model",
                    "Unable to load blockstate", "outside of the generating region",
                    "java.lang.AssertionError", "java.lang.IllegalStateException:"):
        if problem in text:
            raise ValueError("Unexpected native failure: " + problem)
    return sorted(headers)
