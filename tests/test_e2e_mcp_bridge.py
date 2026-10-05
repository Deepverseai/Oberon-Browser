import json
import os
import sys
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MCP_DIR = os.path.join(REPO_ROOT, "mcp_servers", "oberon_browser_mcp")
sys.path.insert(0, MCP_DIR)
import server

class MockFullAgentHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"status":"ok","service":"Oberon Agent Bridge","version":"1.0.0"}')

    def do_POST(self):
        token = self.headers.get("X-Agent-Token")
        if token != server.DEFAULT_TOKEN:
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b'{"status":"error","message":"Unauthorized"}')
            return

        length = int(self.headers.get("Content-Length", 0))
        data = json.loads(self.rfile.read(length).decode("utf-8"))
        action = data.get("action")

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        response = {
            "status": "ok",
            "action": action,
            "text": "Oberon Agentic Web Preview OK",
            "html": "<!DOCTYPE html><html><body>Oberon Live</body></html>",
            "image_base64": "fake_base64_data",
            "mode": data.get("mode", "website"),
            "logs": ["Navigation finished", "DOM loaded without errors"]
        }
        self.wfile.write(json.dumps(response).encode("utf-8"))

    def log_message(self, format, *args):
        pass


class TestE2eMcpBridge(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = HTTPServer(("127.0.0.1", 8770), MockFullAgentHandler)
        cls.thread = threading.Thread(target=cls.server.serve_forever)
        cls.thread.daemon = True
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def test_full_e2e_cycle(self):
        bridge = server.OberonClientBridge(port=8770, auto_wake=False)
        self.assertTrue(bridge.ping())

        # Test Dual Preview toggle
        app_mode = bridge.set_preview_mode("app")
        self.assertEqual(app_mode.get("mode"), "app")

        web_mode = bridge.set_preview_mode("website")
        self.assertEqual(web_mode.get("mode"), "website")

        # Test DOM actions
        nav = bridge.open_url("http://127.0.0.1:3000")
        self.assertEqual(nav.get("status"), "ok")

        click = bridge.click_element("#submit")
        self.assertEqual(click.get("status"), "ok")

        typed = bridge.type_text("#input", "Test Automation")
        self.assertEqual(typed.get("status"), "ok")

        screen = bridge.screenshot()
        self.assertEqual(screen.get("image_base64"), "fake_base64_data")

        # Test Automated Live Audit
        audit = bridge.audit_preview("http://127.0.0.1:3000")
        self.assertTrue(audit.get("success"))
        self.assertEqual(audit.get("preview_mode"), "app")
        self.assertIn("logs", audit)

if __name__ == "__main__":
    unittest.main()
