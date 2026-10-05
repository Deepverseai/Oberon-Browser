import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestWebViewEngine(unittest.TestCase):
    def test_engine_files_exist(self):
        browser_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "browser")
        oberon_client = os.path.join(browser_pkg, "OberonClient.kt")
        oberon_chrome = os.path.join(browser_pkg, "OberonChromeClient.kt")
        file_uploader = os.path.join(browser_pkg, "FileUploaderManager.kt")
        ssl_manager = os.path.join(browser_pkg, "SslSecurityManager.kt")
        main_activity = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "MainActivity.kt")
        activity_main_xml = os.path.join(REPO_ROOT, "app", "src", "main", "res", "layout", "activity_main.xml")

        self.assertTrue(os.path.exists(oberon_client), f"Missing {oberon_client}")
        self.assertTrue(os.path.exists(oberon_chrome), f"Missing {oberon_chrome}")
        self.assertTrue(os.path.exists(file_uploader), f"Missing {file_uploader}")
        self.assertTrue(os.path.exists(ssl_manager), f"Missing {ssl_manager}")
        self.assertTrue(os.path.exists(main_activity), f"Missing {main_activity}")
        self.assertTrue(os.path.exists(activity_main_xml), f"Missing {activity_main_xml}")

    def test_client_and_activity_features(self):
        browser_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "browser")
        oberon_client = os.path.join(browser_pkg, "OberonClient.kt")
        main_activity = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "MainActivity.kt")

        if not os.path.exists(oberon_client) or not os.path.exists(main_activity):
            self.fail("Files not created yet")

        with open(oberon_client, "r", encoding="utf-8") as f:
            client_code = f.read()
        self.assertIn("class OberonClient", client_code)
        self.assertIn("shouldOverrideUrlLoading", client_code)
        self.assertIn("onReceivedSslError", client_code)
        self.assertIn("cleanUserAgent", client_code)

        with open(main_activity, "r", encoding="utf-8") as f:
            act_code = f.read()
        self.assertIn("class MainActivity", act_code)
        self.assertIn("OnBackPressedCallback", act_code) # Android 13/14 back gesture
        self.assertIn("tabManager", act_code)
        self.assertIn("SwipeRefreshLayout", act_code)

if __name__ == "__main__":
    unittest.main()
