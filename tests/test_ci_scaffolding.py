import os
import unittest
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestScaffoldingAndCI(unittest.TestCase):
    def test_workflow_file(self):
        wf_path = os.path.join(REPO_ROOT, ".github", "workflows", "build-apk.yml")
        self.assertTrue(os.path.exists(wf_path), f"Missing CI workflow file: {wf_path}")
        with open(wf_path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("ubuntu-latest", content)
        self.assertIn("actions/setup-java", content)
        self.assertIn("assembleRelease", content)
        self.assertIn("actions/upload-artifact@v4", content)
        self.assertIn("oberon-browser-apks.zip", content)

    def test_settings_gradle(self):
        path = os.path.join(REPO_ROOT, "settings.gradle.kts")
        self.assertTrue(os.path.exists(path), f"Missing settings.gradle.kts: {path}")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn('rootProject.name = "oberon-browser"', content)
        self.assertIn('include(":app")', content)

    def test_root_build_gradle(self):
        path = os.path.join(REPO_ROOT, "build.gradle.kts")
        self.assertTrue(os.path.exists(path), f"Missing root build.gradle.kts: {path}")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("com.android.application", content)
        self.assertIn("org.jetbrains.kotlin.android", content)

    def test_app_build_gradle(self):
        path = os.path.join(REPO_ROOT, "app", "build.gradle.kts")
        self.assertTrue(os.path.exists(path), f"Missing app/build.gradle.kts: {path}")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn('namespace = "com.antigravity.oberon"', content)
        self.assertIn("compileSdk = 34", content)
        self.assertIn("minSdk = 26", content)
        self.assertIn("targetSdk = 34", content)
        self.assertIn("isMinifyEnabled = true", content)

    def test_manifest(self):
        path = os.path.join(REPO_ROOT, "app", "src", "main", "AndroidManifest.xml")
        self.assertTrue(os.path.exists(path), f"Missing AndroidManifest.xml: {path}")
        tree = ET.parse(path)
        root = tree.getroot()
        
        # Check permissions
        perms = [elem.attrib.get("{http://schemas.android.com/apk/res/android}name") for elem in root.findall("uses-permission")]
        self.assertIn("android.permission.INTERNET", perms)
        self.assertIn("android.permission.ACCESS_NETWORK_STATE", perms)
        self.assertIn("android.permission.POST_NOTIFICATIONS", perms)

        # Check Application config
        app = root.find("application")
        self.assertIsNotNone(app)
        sec_config = app.attrib.get("{http://schemas.android.com/apk/res/android}networkSecurityConfig")
        self.assertEqual(sec_config, "@xml/network_security_config")

        # Check FileProvider
        providers = app.findall("provider")
        file_provider = any(p.attrib.get("{http://schemas.android.com/apk/res/android}name") == "androidx.core.content.FileProvider" for p in providers)
        self.assertTrue(file_provider, "androidx.core.content.FileProvider not registered in AndroidManifest.xml")

    def test_network_security_config(self):
        path = os.path.join(REPO_ROOT, "app", "src", "main", "res", "xml", "network_security_config.xml")
        self.assertTrue(os.path.exists(path), f"Missing network_security_config.xml: {path}")
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("127.0.0.1", content)
        self.assertIn("localhost", content)
        self.assertIn('cleartextTrafficPermitted="true"', content)

    def test_file_paths(self):
        path = os.path.join(REPO_ROOT, "app", "src", "main", "res", "xml", "file_paths.xml")
        self.assertTrue(os.path.exists(path), f"Missing file_paths.xml: {path}")

    def test_colors_and_strings(self):
        colors_path = os.path.join(REPO_ROOT, "app", "src", "main", "res", "values", "colors.xml")
        strings_path = os.path.join(REPO_ROOT, "app", "src", "main", "res", "values", "strings.xml")
        self.assertTrue(os.path.exists(colors_path), f"Missing colors.xml: {colors_path}")
        self.assertTrue(os.path.exists(strings_path), f"Missing strings.xml: {strings_path}")
        
        with open(strings_path, "r", encoding="utf-8") as f:
            str_content = f.read()
        self.assertIn("Oberon Browser", str_content)

        with open(colors_path, "r", encoding="utf-8") as f:
            col_content = f.read()
        # Check Rich Neutral color values
        self.assertIn("#090A0B", col_content) # Deep Obsidian
        self.assertIn("#131416", col_content) # Warm Charcoal

if __name__ == "__main__":
    unittest.main()
