#!/usr/bin/env python3
"""
Oberon Browser MCP Bridge Server
Enables Antigravity CLI and autonomous AI agents to preview, control, and audit
web applications running on Android device via Oberon Browser's embedded bridge.
"""

import json
import os
import subprocess
import sys
import time
import urllib.request
import urllib.error

DEFAULT_PORT = 8765
DEFAULT_HOST = "127.0.0.1"
DEFAULT_TOKEN = "oberon_local_secret_token"

class OberonClientBridge:
    def __init__(self, host=DEFAULT_HOST, port=DEFAULT_PORT, token=DEFAULT_TOKEN, auto_wake=True):
        self.host = host
        self.port = port
        self.token = token
        self.base_url = f"http://{host}:{port}"
        self.auto_wake = auto_wake

    def _send_command(self, payload: dict) -> dict:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            self.base_url,
            data=data,
            headers={
                "Content-Type": "application/json",
                "X-Agent-Token": self.token
            },
            method="POST"
        )
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                res_body = response.read().decode("utf-8")
                return json.loads(res_body)
        except (urllib.error.URLError, ConnectionRefusedError, TimeoutError) as e:
            if self.auto_wake:
                self.wake_app()
                time.sleep(1.5)
                # Retry once after waking app
                try:
                    with urllib.request.urlopen(req, timeout=5) as response:
                        res_body = response.read().decode("utf-8")
                        return json.loads(res_body)
                except Exception as retry_err:
                    return {"status": "error", "message": f"Failed after wake: {retry_err}"}
            return {"status": "error", "message": str(e)}

    def ping(self) -> bool:
        req = urllib.request.Request(
            self.base_url,
            headers={"X-Agent-Token": self.token},
            method="GET"
        )
        try:
            with urllib.request.urlopen(req, timeout=2) as response:
                return response.status == 200
        except Exception:
            return False

    def wake_app(self, initial_url: str = "http://127.0.0.1:8765"):
        """Auto-wakes Oberon Browser on Android screen via Activity Manager"""
        try:
            cmd = ["am", "start", "-n", "com.antigravity.oberon/.MainActivity", "-d", initial_url]
            subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=2)
        except Exception:
            pass

    def open_url(self, url: str) -> dict:
        return self._send_command({"action": "navigate", "url": url})

    def new_tab(self, url: str) -> dict:
        return self._send_command({"action": "new_tab", "url": url})

    def click_element(self, selector: str) -> dict:
        return self._send_command({"action": "click", "selector": selector})

    def type_text(self, selector: str, text: str) -> dict:
        return self._send_command({"action": "type", "selector": selector, "text": text})

    def read_page(self) -> dict:
        return self._send_command({"action": "extract_text"})

    def get_html(self) -> dict:
        return self._send_command({"action": "get_html"})

    def screenshot(self) -> dict:
        return self._send_command({"action": "screenshot"})

    def set_preview_mode(self, mode: str) -> dict:
        """mode: 'website' or 'app'"""
        return self._send_command({"action": "set_preview_mode", "mode": mode})

    def get_telemetry(self) -> dict:
        return self._send_command({"action": "get_telemetry"})

    def audit_preview(self, url: str) -> dict:
        """Automated end-to-end audit: switches to app preview mode, opens URL, checks errors"""
        self.set_preview_mode("app")
        nav_res = self.open_url(url)
        time.sleep(1.0)
        page_content = self.read_page()
        telemetry = self.get_telemetry()

        return {
            "success": nav_res.get("status") == "ok",
            "url": url,
            "preview_mode": "app",
            "page_text_sample": page_content.get("text", "")[:300],
            "logs": telemetry.get("logs", [])
        }


# MCP Server entrypoint when executed directly or via stdio MCP host
if __name__ == "__main__":
    bridge = OberonClientBridge()
    if len(sys.argv) > 1:
        action = sys.argv[1]
        if action == "ping":
            print("Online" if bridge.ping() else "Offline")
        elif action == "open" and len(sys.argv) > 2:
            print(json.dumps(bridge.open_url(sys.argv[2]), indent=2))
        elif action == "audit" and len(sys.argv) > 2:
            print(json.dumps(bridge.audit_preview(sys.argv[2]), indent=2))
        else:
            print(f"Unknown action: {action}")
    else:
        print("Oberon Browser MCP Bridge CLI ready. Run with 'ping', 'open <url>', or 'audit <url>'.")
