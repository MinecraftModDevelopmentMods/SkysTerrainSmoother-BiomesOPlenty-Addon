"""Negative controls for the exact Forge 40 native log classification."""

import unittest
import json
import hashlib
from pathlib import Path
import shutil
import tempfile
import tomllib
from native_client_log import audit, classify


LOGIN = "[Client thread/INFO] [NetworkHooks/]: Connected to a modded server."
START = "[Server thread/INFO] [MinecraftServer/]: Preparing start region for dimension minecraft:overworld"
NOTICE = "[Thread-1/FATAL] [net.minecraftforge.common.ForgeConfig/CORE]: Forge config just got changed on the file system!"
PAYLOAD = "[Client thread/ERROR] [net.minecraftforge.fml.network.simple.IndexedMessageCodec/SIMPLENET]: Received empty payload on channel fml:handshake"


class NativeClientLogTest(unittest.TestCase):
    def gateway_block(self):
        root = Path(__file__).resolve().parent.parent
        fixture = root / "src/test/resources/native-authlib-gateway.txt"
        control = json.loads((root / "tools/native-log-profiles.json").read_text())["offline_profile_gateway_control"]
        self.assertTrue(control["library_only_control"] and control["unchanged_offline_profile"])
        self.assertEqual(control["fixture_sha256"], hashlib.sha256(fixture.read_bytes()).hexdigest().upper())
        self.assertEqual(control["authlib_sha256"], "21132E3BDE5EEAC556437DA9580221931BB00DBF350B453F100526C9AA232FEE")
        return fixture.read_text()

    def completed_client(self, block):
        return "\n".join((START, LOGIN, block,
                           "[time] [Render thread/INFO] [Probe/]: TERRAIN_SMOOTHER_CLIENT_PASS",
                           "[time] [Render thread/INFO] [Probe/]: TERRAIN_SMOOTHER_CLIENT_WORLD_PASS",
                           "[time] [Server thread/INFO] [Probe/]: TERRAIN_SMOOTHER_RUNTIME_PASS phase=reload"))

    def test_controlled_profile_gateway_failure_requires_successful_native_completion(self):
        receipt = classify(self.completed_client(self.gateway_block()), True)
        self.assertEqual(receipt["notices"], [])
        for marker in ("TERRAIN_SMOOTHER_CLIENT_PASS", "TERRAIN_SMOOTHER_CLIENT_WORLD_PASS",
                       "TERRAIN_SMOOTHER_RUNTIME_PASS phase=reload"):
            with self.subTest(marker=marker), self.assertRaises(ValueError):
                classify(self.completed_client(self.gateway_block()).replace(marker, "incomplete"), True)

    def test_other_profiles_loggers_threads_and_cause_chains_still_fail(self):
        block = self.gateway_block()
        changes = (
            ("Render thread/WARN", "Render thread/ERROR"),
            ("Render thread/WARN", "Server thread/WARN"),
            ("YggdrasilMinecraftSessionService/", "OtherSessionService/"),
            ("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222"),
            ("name=TerrainSmootherChecks", "name=AnotherPlayer"),
            ("authlib-3.3.39.jar", "authlib-4.0.0.jar"),
            ("Caused by: com.google.gson.JsonSyntaxException:", "Caused by: AnotherException:"),
            ("Expected BEGIN_OBJECT but was STRING", "An unrelated illegal state"),
        )
        for old, new in changes:
            changed = block.replace(old, new)
            self.assertNotEqual(block, changed)
            with self.subTest(change=new), self.assertRaises(ValueError):
                classify(self.completed_client(changed), True)

    def test_gateway_warning_cannot_hide_an_unrelated_failure(self):
        log = self.completed_client(self.gateway_block())
        for extra in ("java.lang.IllegalStateException: A genuine world failure",
                      "[time] [Server thread/ERROR] [MinecraftServer/]: Failed to save a chunk",
                      "java.lang.NoClassDefFoundError: missing.class"):
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                classify(log + "\n" + extra, True)
        with self.assertRaises(ValueError):
            classify(self.completed_client(self.gateway_block() + self.gateway_block()), True)

    def test_previous_target_notices_are_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((NOTICE, START, LOGIN, PAYLOAD)), True)

    def test_clean_native_startup_is_accepted(self):
        receipt = classify("\n".join((START, LOGIN)), True)
        self.assertEqual(receipt["native_config_notices"], 0)
        self.assertEqual(receipt["empty_handshakes"], 0)
        self.assertEqual(receipt["notices"], [])

    def test_actual_config_change_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((NOTICE, START, LOGIN)), False)

    def test_notice_after_world_start_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((START, NOTICE, LOGIN)), True)

    def test_other_fatal_message_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((NOTICE.replace("just got changed", "failed"), START, LOGIN)), True)

    def test_other_logger_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((NOTICE.replace("ForgeConfig/CORE", "OtherConfig/CORE"), START, LOGIN)), True)

    def test_repeated_config_notifications_are_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((NOTICE, NOTICE, NOTICE, START, LOGIN)), True)

    def test_other_handshake_error_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((START, LOGIN, PAYLOAD.replace("fml:handshake", "another:channel"))), True)

    def test_handshake_failure_without_login_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((START, PAYLOAD)), True)

    def test_stderr_is_rejected(self):
        with self.assertRaises(ValueError):
            classify("\n".join((START, LOGIN, "[STDERR/]: java.lang.IllegalStateException")), True)


class NativeClientFilesTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        defaults = Path(__file__).resolve().parent.parent / "src/bootstrapTest/defaultconfigs"
        shutil.copytree(defaults / "config", self.directory / "config")
        self.server = self.directory / "saves/world/serverconfig"
        self.server.mkdir(parents=True)
        shutil.copyfile(defaults / "defaultconfigs/forge-server.toml", self.server / "forge-server.toml")
        (self.directory / "logs").mkdir()
        (self.directory / "logs/latest.log").write_text("\n".join((
            START, LOGIN,
            "FORGE40_CLIENT_CONTROL_PASS joined=true sky_mods=false")), encoding="utf-8")

    def test_native_defaults_with_different_order_and_comments(self):
        client = self.directory / "config/forge-client.toml"
        settings = tomllib.loads(client.read_text(encoding="utf-8"))["client"]
        client.write_text("[client]\n" + "\n".join(
            name + " = " + json.dumps(value) for name, value in reversed(list(settings.items()))),
            encoding="utf-8")
        receipt = audit(self.directory, "control", True)
        self.assertTrue(receipt["unchanged_native_defaults"])
        self.assertEqual(receipt["native_config_notices"], 0)

    def test_changed_default_stops_the_check(self):
        client = self.directory / "config/forge-client.toml"
        contents = client.read_text(encoding="utf-8")
        self.assertIn("experimentalForgeLightPipelineEnabled = false", contents)
        client.write_text(contents.replace("experimentalForgeLightPipelineEnabled = false",
                                           "experimentalForgeLightPipelineEnabled = true"), encoding="utf-8")
        with self.assertRaises(ValueError):
            audit(self.directory, "control", True)

    def test_missing_server_config_stops_the_check(self):
        (self.server / "forge-server.toml").unlink()
        with self.assertRaises(ValueError):
            audit(self.directory, "control", True)

    def test_crash_report_stops_the_check(self):
        (self.directory / "crash-reports").mkdir()
        (self.directory / "crash-reports/test.txt").write_text("Simulated failure")
        with self.assertRaises(ValueError):
            audit(self.directory, "control", True)


class NativeRegistryProfileTest(unittest.TestCase):
    def test_server_proof_does_not_authorise_client_notices(self):
        import json
        import tempfile
        from native_registry_notices import profile_for

        root = Path(__file__).resolve().parent.parent
        policy = json.loads((root / "tools/forge-45-log-profiles.json").read_text(encoding="utf-8"))
        profile = policy["profiles"]["BA2421F2A9D2E746401DF3FDA2AAB524E8E8A2537F6332815C9AA913BD72E161"]
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            manifest = directory / "source-inputs.json"
            source = {
                "world_sha256": profile["sha256"],
                "manifest_sha256": profile["manifest_sha256"],
                "mode": "server",
            }
            manifest.write_text(json.dumps(source), encoding="utf-8")
            self.assertEqual(profile_for(directory), profile)

            source["mode"] = "client"
            manifest.write_text(json.dumps(source), encoding="utf-8")
            self.assertIsNone(profile_for(directory))
            (directory / "artifact-inputs.json").write_text(
                json.dumps(profile["source_artifacts"]), encoding="utf-8")
            self.assertIsNone(profile_for(directory))


if __name__ == "__main__":
    unittest.main()
