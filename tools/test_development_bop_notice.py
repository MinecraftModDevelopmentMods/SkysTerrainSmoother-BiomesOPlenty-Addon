"""The development allowance must not hide other errors or affect packaged games."""

import unittest

from development_bop_notice import CONTROL, checked_notice
from native_registry_notices import audit_text


class DevelopmentBopNoticeTest(unittest.TestCase):
    def setUp(self):
        self.notice = ("[time] [Server thread/ERROR] [ne.ne.ne.se.ServerLifecycleHooks/]: "
                       + CONTROL["message"] + "\n\t - minecraft:allay\n")

    def test_exact_pair_is_separate_from_other_log_lines(self):
        clean, count = checked_notice("before\n" + self.notice + "after\n")
        self.assertEqual((clean, count), ("before\nafter\n", 1))
        self.assertEqual(checked_notice("clean\n"), ("clean\n", 0))

    def test_full_logger_and_crlf_are_recognised(self):
        full = self.notice.replace("ne.ne.ne.se", "net.neoforged.neoforge.server")
        self.assertEqual(checked_notice(full.replace("\n", "\r\n")), ("", 1))

    def test_changed_repeated_or_other_entity_notices_fail(self):
        for notice in (self.notice * 2,
                       self.notice.replace("minecraft:allay", "minecraft:zombie"),
                       self.notice.replace("ServerLifecycleHooks", "AnotherLogger"),
                       self.notice.replace("Server thread", "Render thread"),
                       self.notice + "\t - minecraft:zombie\n"):
            with self.subTest(notice=notice), self.assertRaises(ValueError):
                checked_notice(notice)

    def test_other_errors_and_changed_message_still_fail_strict_audit(self):
        for text in (self.notice + "[time] [Server thread/ERROR] [Other/]: failed\n",
                     self.notice.replace("any restrictions.", "different restrictions.")):
            with self.subTest(text=text), self.assertRaises(ValueError):
                audit_text(checked_notice(text)[0])

    def test_packaged_audit_does_not_accept_this_notice(self):
        with self.assertRaises(ValueError):
            audit_text(self.notice)


if __name__ == "__main__":
    unittest.main()
