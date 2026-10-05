import json
import os
import unittest
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class MockOberonAgentHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"status":"ok","service":"Oberon Agent Bridge"}')

    def do_POST(self):
        token = self.headers.get("X-Agent-Token")
        if token != "oberon_local_secret_token":
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b'{"status":"error","message":"Unauthorized"}')
            return

        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length).decode("utf-8")
        req = json.loads(post_data)

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        action = req.get("action")
        if action == "navigate":
            resp = {"status": "ok", "url": req.get("url")}
        elif action == "click":
            resp = {"status": "ok", "result": "clicked"}
        elif action == "type":
            resp = {"status": "ok", "result": "typed"}
        elif action == "extract_text":
            resp = {"status": "ok", "text": "Page body content"}
        elif action == "get_html":
            resp = {"status": "ok", "html": "<html><body>Page body content</body></html>"}
        elif action == "screenshot":
            resp = {"status": "ok", "image_base64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="}
        elif action == "set_preview_mode":
            resp = {"status": "ok", "mode": req.get("mode")}
        elif action == "get_telemetry":
            resp = {"status": "ok", "logs": ["Page loaded"]}
        else:
            resp = {"status": "error", "message": "Unknown action"}

        self.wfile.write(json.dumps(resp).encode("utf-8"))

    def log_message(self, format, *args):
        pass # Suppress test server logs


class TestOberonMcpBridge(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mock_server = HTTPServer(("127.0.0.1", 8769), MockOberonAgentHandler)
        cls.server_thread = threading.Thread(target=cls.mock_server.serve_forever)
        cls.server_thread.daemon = True
        cls.server_thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.mock_server.shutdown()
        cls.mock_server.server_close()

    def test_mcp_files_exist(self):
        mcp_dir = os.path.join(REPO_ROOT, "mcp_servers", "oberon_browser_mcp")
        server_py = os.path.join(mcp_dir, "server.py")
        reqs_txt = os.path.join(mcp_dir, "requirements.txt")

        self.assertTrue(os.path.exists(server_py), f"Missing {server_py}")
        self.assertTrue(os.path.exists(reqs_txt), f"Missing {reqs_txt}")

    def test_mcp_bridge_client_calls(self):
        mcp_dir = os.path.join(REPO_ROOT, "mcp_servers", "oberon_browser_mcp")
        server_py = os.path.join(mcp_dir, "server.py")
        if not os.path.exists(server_py):
            self.fail("server.py does not exist yet")

        import sys
        sys.path.insert(0, mcp_dir)
        import server

        client = server.OberonClientBridge(port=8769, auto_wake=False)
        self.assertTrue(client.ping())

        # Test tool implementations
        nav_res = client.open_url("http://localhost:3000")
        self.assertEqual(nav_res.get("status"), "ok")

        click_res = client.click_element("#login-btn")
        self.assertEqual(click_res.get("status"), "ok")

        text_res = client.read_page()
        self.assertIn("Page body content", text_res.get("text", ""))

        preview_res = client.set_preview_mode("app")
        self.assertEqual(preview_res.get("mode"), "app")

        # Test audit preview function
        audit_report = client.audit_preview("http://localhost:3000")
        self.assertTrue(audit_report.get("success"))
        self.assertIn("logs", audit_report)

if __name__ == "__main__":
    unittest.main()
