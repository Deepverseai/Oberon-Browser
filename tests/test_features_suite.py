import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestFeaturesSuite(unittest.TestCase):
    def test_feature_files_exist(self):
        feat_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "features")
        ad_blocker = os.path.join(feat_pkg, "AdBlocker.kt")
        find_in_page = os.path.join(feat_pkg, "FindInPageManager.kt")
        pdf_exporter = os.path.join(feat_pkg, "PdfExporter.kt")
        nav_stack = os.path.join(feat_pkg, "NavigationStackManager.kt")
        layout_find = os.path.join(REPO_ROOT, "app", "src", "main", "res", "layout", "layout_find_in_page.xml")

        self.assertTrue(os.path.exists(ad_blocker), f"Missing {ad_blocker}")
        self.assertTrue(os.path.exists(find_in_page), f"Missing {find_in_page}")
        self.assertTrue(os.path.exists(pdf_exporter), f"Missing {pdf_exporter}")
        self.assertTrue(os.path.exists(nav_stack), f"Missing {nav_stack}")
        self.assertTrue(os.path.exists(layout_find), f"Missing {layout_find}")

    def test_ad_blocker_rules(self):
        feat_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "features")
        ad_blocker_kt = os.path.join(feat_pkg, "AdBlocker.kt")
        if not os.path.exists(ad_blocker_kt):
            self.fail("AdBlocker.kt not found")

        with open(ad_blocker_kt, "r", encoding="utf-8") as f:
            code = f.read()

        self.assertIn("object AdBlocker", code)
        self.assertIn("fun isAd(url: String): Boolean", code)
        # Check standard ad network signatures
        self.assertIn("doubleclick.net", code)
        self.assertIn("googleads", code)
        self.assertIn("adnxs.com", code)

    def test_pdf_and_find_in_page_contracts(self):
        feat_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "features")
        pdf_exporter_kt = os.path.join(feat_pkg, "PdfExporter.kt")
        find_in_page_kt = os.path.join(feat_pkg, "FindInPageManager.kt")

        if not os.path.exists(pdf_exporter_kt) or not os.path.exists(find_in_page_kt):
            self.fail("Files missing")

        with open(pdf_exporter_kt, "r", encoding="utf-8") as f:
            pdf_code = f.read()
        self.assertIn("PrintManager", pdf_code)
        self.assertIn("createPrintDocumentAdapter", pdf_code)

        with open(find_in_page_kt, "r", encoding="utf-8") as f:
            find_code = f.read()
        self.assertIn("findAllAsync", find_code)
        self.assertIn("findNext", find_code)

if __name__ == "__main__":
    unittest.main()
