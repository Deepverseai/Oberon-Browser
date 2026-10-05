import json
import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestAgentBridgeAndPreview(unittest.TestCase):
    def test_bridge_files_exist(self):
        agent_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "agent")
        preview_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "preview")

        server_kt = os.path.join(agent_pkg, "AgentServer.kt")
        protocol_kt = os.path.join(agent_pkg, "AutomationProtocol.kt")
        preview_mgr_kt = os.path.join(preview_pkg, "PreviewModeManager.kt")
        cursor_kt = os.path.join(preview_pkg, "VirtualCursor.kt")

        self.assertTrue(os.path.exists(server_kt), f"Missing {server_kt}")
        self.assertTrue(os.path.exists(protocol_kt), f"Missing {protocol_kt}")
        self.assertTrue(os.path.exists(preview_mgr_kt), f"Missing {preview_mgr_kt}")
        self.assertTrue(os.path.exists(cursor_kt), f"Missing {cursor_kt}")

    def test_agent_server_security_and_protocol(self):
        agent_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "agent")
        server_kt = os.path.join(agent_pkg, "AgentServer.kt")
        protocol_kt = os.path.join(agent_pkg, "AutomationProtocol.kt")

        if not os.path.exists(server_kt) or not os.path.exists(protocol_kt):
            self.fail("Agent files not created yet")

        with open(server_kt, "r", encoding="utf-8") as f:
            server_code = f.read()

        # Binding constraint: MUST bind strictly to 127.0.0.1 on port 8765
        self.assertIn("8765", server_code)
        self.assertIn("127.0.0.1", server_code)
        # Authentication header check
        self.assertIn("X-Agent-Token", server_code)

        with open(protocol_kt, "r", encoding="utf-8") as f:
            proto_code = f.read()

        # Action support
        self.assertIn("navigate", proto_code)
        self.assertIn("click", proto_code)
        self.assertIn("type", proto_code)
        self.assertIn("extract_text", proto_code)
        self.assertIn("get_html", proto_code)
        self.assertIn("screenshot", proto_code)
        self.assertIn("set_preview_mode", proto_code)
        self.assertIn("get_telemetry", proto_code)

    def test_virtual_cursor_and_preview_modes(self):
        preview_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "preview")
        preview_mgr_kt = os.path.join(preview_pkg, "PreviewModeManager.kt")
        cursor_kt = os.path.join(preview_pkg, "VirtualCursor.kt")

        if not os.path.exists(preview_mgr_kt) or not os.path.exists(cursor_kt):
            self.fail("Preview files not created yet")

        with open(preview_mgr_kt, "r", encoding="utf-8") as f:
            preview_code = f.read()
        self.assertIn("enum class PreviewMode", preview_code)
        self.assertIn("WEBSITE", preview_code)
        self.assertIn("APP", preview_code)
        self.assertIn("390", preview_code) # Mobile device viewport width
        self.assertIn("844", preview_code) # Mobile device viewport height

        with open(cursor_kt, "r", encoding="utf-8") as f:
            cursor_code = f.read()
        self.assertIn("__oberon_cursor__", cursor_code)
        self.assertIn("glideCursor", cursor_code)
        self.assertIn("showClickRipple", cursor_code)

if __name__ == "__main__":
    unittest.main()
