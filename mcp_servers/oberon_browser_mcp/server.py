#!/usr/bin/env python3
"""
Oberon Browser MCP Bridge Server
Enables Antigravity CLI and autonomous AI agents to preview, control, and audit
web applications running on Android device via Oberon Browser's embedded bridge.
Provides standard Model Context Protocol (MCP) JSON-RPC 2.0 stdio server.
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

    def wake_app(self, initial_url: str = None) -> bool:
        """Auto-wakes Oberon Browser on Android screen via Activity Manager"""
        am_bin = "/data/data/com.termux/files/usr/bin/am" if os.path.exists("/data/data/com.termux/files/usr/bin/am") else "am"
        packages = ["com.antigravity.oberon.debug", "com.antigravity.oberon"]

        for pkg in packages:
            cmd = [am_bin, "start", "--user", "0", "-n", f"{pkg}/com.antigravity.oberon.MainActivity"]
            if initial_url:
                cmd.extend(["-d", initial_url])
            try:
                res = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=4)
                if res.returncode == 0:
                    return True
            except Exception:
                pass
        return False

    def open_url(self, url: str) -> dict:
        res = self._send_command({"action": "navigate", "url": url})
        # If embedded HTTP bridge is not yet active, fallback to direct Android Activity intent
        if res.get("status") == "error":
            woke = self.wake_app(initial_url=url)
            if woke:
                return {
                    "status": "ok",
                    "url": url,
                    "mode": "intent_dispatch",
                    "note": "URL dispatched via Android Activity intent directly to Oberon screen."
                }
        return res

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


def run_mcp_server():
    """Standard Model Context Protocol (MCP) JSON-RPC 2.0 server over stdio"""
    bridge = OberonClientBridge()

    tools = [
        {
            "name": "oberon_status",
            "description": "Check Oberon Browser connection status and wake up on Android screen if needed.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "wake_if_offline": {
                        "type": "boolean",
                        "description": "If true, attempts to wake Oberon Browser on screen when offline."
                    }
                },
                "required": []
            }
        },
        {
            "name": "oberon_open_url",
            "description": "Open a website or web app URL in Oberon Browser on Android device.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "The URL to navigate to (e.g. http://localhost:3000 or https://example.com)"
                    }
                },
                "required": ["url"]
            }
        },
        {
            "name": "oberon_new_tab",
            "description": "Open a new browser tab in Oberon with the given URL.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "URL for the new tab (defaults to homepage if blank)"
                    }
                },
                "required": []
            }
        },
        {
            "name": "oberon_click",
            "description": "Click an element by CSS selector with animated virtual cursor feedback in Oberon Browser.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "selector": {
                        "type": "string",
                        "description": "CSS selector of the element to click (e.g. #login-btn, button.primary)"
                    }
                },
                "required": ["selector"]
            }
        },
        {
            "name": "oberon_type",
            "description": "Type text into an input or textarea element by CSS selector.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "selector": {
                        "type": "string",
                        "description": "CSS selector of the input field"
                    },
                    "text": {
                        "type": "string",
                        "description": "Text to type into the field"
                    }
                },
                "required": ["selector", "text"]
            }
        },
        {
            "name": "oberon_read_page",
            "description": "Extract rendered visible text content from currently active webpage in Oberon.",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "required": []
            }
        },
        {
            "name": "oberon_get_html",
            "description": "Get full DOM HTML content of currently active webpage in Oberon.",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "required": []
            }
        },
        {
            "name": "oberon_screenshot",
            "description": "Capture base64-encoded PNG screenshot of current page view in Oberon.",
            "inputSchema": {
                "type": "object",
                "properties": {},
                "required": []
            }
        },
        {
            "name": "oberon_set_preview_mode",
            "description": "Toggle between responsive 'website' and mobile device frame 'app' (390x844 dp) preview mode.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "mode": {
                        "type": "string",
                        "enum": ["website", "app"],
                        "description": "'website' for full screen responsive, 'app' for simulated mobile device frame"
                    }
                },
                "required": ["mode"]
            }
        },
        {
            "name": "oberon_audit",
            "description": "Perform an automated audit on a web application in Oberon Browser (switches mode, checks errors, extracts DOM).",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "url": {
                        "type": "string",
                        "description": "URL to audit"
                    }
                },
                "required": ["url"]
            }
        }
    ]

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except Exception:
            continue

        req_id = req.get("id")
        method = req.get("method")
        params = req.get("params", {})

        if method == "initialize":
            resp = {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {"tools": {}},
                    "serverInfo": {
                        "name": "oberon-browser-mcp",
                        "version": "1.0.0"
                    }
                }
            }
        elif method == "notifications/initialized":
            continue
        elif method == "tools/list":
            resp = {
                "jsonrpc": "2.0",
                "id": req_id,
                "result": {"tools": tools}
            }
        elif method == "tools/call":
            tool_name = params.get("name")
            args = params.get("arguments", {})

            try:
                if tool_name == "oberon_status":
                    online = bridge.ping()
                    woke = False
                    if not online and args.get("wake_if_offline"):
                        woke = bridge.wake_app()
                        time.sleep(1.0)
                        online = bridge.ping()
                    res_data = {
                        "online": online,
                        "bridge_url": bridge.base_url,
                        "auto_wake_tested": woke,
                        "status": "ready" if online else "offline_or_background"
                    }
                elif tool_name == "oberon_open_url":
                    res_data = bridge.open_url(args.get("url", ""))
                elif tool_name == "oberon_new_tab":
                    res_data = bridge.new_tab(args.get("url", ""))
                elif tool_name == "oberon_click":
                    res_data = bridge.click_element(args.get("selector", ""))
                elif tool_name == "oberon_type":
                    res_data = bridge.type_text(args.get("selector", ""), args.get("text", ""))
                elif tool_name == "oberon_read_page":
                    res_data = bridge.read_page()
                elif tool_name == "oberon_get_html":
                    res_data = bridge.get_html()
                elif tool_name == "oberon_screenshot":
                    res_data = bridge.screenshot()
                elif tool_name == "oberon_set_preview_mode":
                    res_data = bridge.set_preview_mode(args.get("mode", "website"))
                elif tool_name == "oberon_audit":
                    res_data = bridge.audit_preview(args.get("url", ""))
                else:
                    res_data = {"status": "error", "message": f"Unknown tool: {tool_name}"}

                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "result": {
                        "content": [
                            {"type": "text", "text": json.dumps(res_data, indent=2)}
                        ]
                    }
                }
            except Exception as e:
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "error": {"code": -32000, "message": str(e)}
                }
        else:
            resp = {
                "jsonrpc": "2.0",
                "id": req_id,
                "error": {"code": -32601, "message": f"Method not found: {method}"}
            }

        sys.stdout.write(json.dumps(resp) + "\n")
        sys.stdout.flush()


if __name__ == "__main__":
    if "--mcp" in sys.argv:
        run_mcp_server()
    elif len(sys.argv) > 1:
        bridge = OberonClientBridge()
        action = sys.argv[1]
        if action == "ping":
            print("Online" if bridge.ping() else "Offline")
        elif action == "open" and len(sys.argv) > 2:
            print(json.dumps(bridge.open_url(sys.argv[2]), indent=2))
        elif action == "audit" and len(sys.argv) > 2:
            print(json.dumps(bridge.audit_preview(sys.argv[2]), indent=2))
        elif action == "wake":
            url = sys.argv[2] if len(sys.argv) > 2 else None
            success = bridge.wake_app(url)
            print("Woken successfully" if success else "Failed to wake")
        else:
            print(f"Unknown action: {action}")
    else:
        # Default when run without tty
        if not sys.stdin.isatty():
            run_mcp_server()
        else:
            print("Oberon Browser MCP Bridge CLI ready. Run with '--mcp', 'ping', 'open <url>', or 'audit <url>'.")
