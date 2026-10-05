import os
import re
import unittest
import urllib.parse

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

# Pure Python prototype mirror of the classification logic for specification verification
def classify_url_or_search(query: str, engine_query_url: str = "https://www.google.com/search?q=%s") -> str:
    s = query.strip()
    if not s:
        return ""
    if s.startswith("http://") or s.startswith("https://") or s.startswith("file://") or s.startswith("content://") or s.startswith("about:"):
        return s
    
    # Check IP address with or without port (e.g., 192.168.1.1, 127.0.0.1:8765)
    ip_pattern = r"^(\d{1,3}\.){3}\d{1,3}(:\d+)?(/.*)?$"
    if re.match(ip_pattern, s):
        return f"http://{s}"

    # Check localhost with or without port (e.g. localhost:3000)
    localhost_pattern = r"^localhost(:\d+)?(/.*)?$"
    if re.match(localhost_pattern, s, re.IGNORECASE):
        return f"http://{s}"

    # Check standard domain format (e.g., github.com, sub.domain.org/path)
    # Must not contain spaces
    domain_pattern = r"^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(:\d+)?(/.*)?$"
    if " " not in s and re.match(domain_pattern, s):
        return f"https://{s}"

    # Otherwise treated as a search query
    encoded = urllib.parse.quote_plus(s)
    return engine_query_url.replace("%s", encoded)


class TestOmniboxClassifier(unittest.TestCase):
    def test_classifier_rules(self):
        # 1. Scheme already present
        self.assertEqual(classify_url_or_search("https://google.com"), "https://google.com")
        self.assertEqual(classify_url_or_search("http://insecure.site"), "http://insecure.site")

        # 2. Domain without scheme defaults to HTTPS
        self.assertEqual(classify_url_or_search("github.com"), "https://github.com")
        self.assertEqual(classify_url_or_search("sub.deepmind.google/path"), "https://sub.deepmind.google/path")

        # 3. Localhost & IP addresses default to HTTP
        self.assertEqual(classify_url_or_search("192.168.1.1:8080"), "http://192.168.1.1:8080")
        self.assertEqual(classify_url_or_search("127.0.0.1:8765"), "http://127.0.0.1:8765")
        self.assertEqual(classify_url_or_search("localhost:3000"), "http://localhost:3000")

        # 4. Search queries
        self.assertEqual(
            classify_url_or_search("latest AI news"),
            "https://www.google.com/search?q=latest+AI+news"
        )
        self.assertEqual(
            classify_url_or_search("python tutorial", "https://duckduckgo.com/?q=%s"),
            "https://duckduckgo.com/?q=python+tutorial"
        )

    def test_kotlin_files_exist(self):
        pkg_dir = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "search")
        classifier_kt = os.path.join(pkg_dir, "OmniboxClassifier.kt")
        engine_mgr_kt = os.path.join(pkg_dir, "SearchEngineManager.kt")
        sugg_client_kt = os.path.join(pkg_dir, "SuggestionsClient.kt")
        layout_xml = os.path.join(REPO_ROOT, "app", "src", "main", "res", "layout", "layout_omnibox.xml")

        self.assertTrue(os.path.exists(classifier_kt), f"Missing {classifier_kt}")
        self.assertTrue(os.path.exists(engine_mgr_kt), f"Missing {engine_mgr_kt}")
        self.assertTrue(os.path.exists(sugg_client_kt), f"Missing {sugg_client_kt}")
        self.assertTrue(os.path.exists(layout_xml), f"Missing {layout_xml}")

        with open(classifier_kt, "r", encoding="utf-8") as f:
            code = f.read()
        self.assertIn("object OmniboxClassifier", code)
        self.assertIn("fun classify(", code)

        with open(engine_mgr_kt, "r", encoding="utf-8") as f:
            code = f.read()
        self.assertIn("class SearchEngineManager", code)
        self.assertIn("data class SearchEngine", code)

        with open(sugg_client_kt, "r", encoding="utf-8") as f:
            code = f.read()
        self.assertIn("object SuggestionsClient", code)

        with open(layout_xml, "r", encoding="utf-8") as f:
            xml = f.read()
        self.assertIn("AutoCompleteTextView", xml)
        self.assertIn("btnVoiceSearch", xml)
        self.assertIn("btnClearQuery", xml)

if __name__ == "__main__":
    unittest.main()
