import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestTabManager(unittest.TestCase):
    def test_tab_files_exist(self):
        tabs_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "tabs")
        ui_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "ui")
        res_layout = os.path.join(REPO_ROOT, "app", "src", "main", "res", "layout")

        tab_model = os.path.join(tabs_pkg, "TabModel.kt")
        tab_manager = os.path.join(tabs_pkg, "TabManager.kt")
        tab_sheet = os.path.join(ui_pkg, "TabSwitcherBottomSheet.kt")
        bs_layout = os.path.join(res_layout, "bottom_sheet_tabs.xml")
        item_layout = os.path.join(res_layout, "item_tab.xml")

        self.assertTrue(os.path.exists(tab_model), f"Missing {tab_model}")
        self.assertTrue(os.path.exists(tab_manager), f"Missing {tab_manager}")
        self.assertTrue(os.path.exists(tab_sheet), f"Missing {tab_sheet}")
        self.assertTrue(os.path.exists(bs_layout), f"Missing {bs_layout}")
        self.assertTrue(os.path.exists(item_layout), f"Missing {item_layout}")

    def test_tab_model_and_manager_contracts(self):
        tab_manager_path = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "tabs", "TabManager.kt")
        if not os.path.exists(tab_manager_path):
            self.fail(f"Missing {tab_manager_path}")

        with open(tab_manager_path, "r", encoding="utf-8") as f:
            code = f.read()

        # Verify TabManager methods
        self.assertIn("fun createTab(", code)
        self.assertIn("fun closeTab(", code)
        self.assertIn("fun selectTab(", code)
        self.assertIn("fun getActiveTab():", code)
        self.assertIn("fun getTabCount():", code)
        # Verify Tab Virtualization (pause background webviews)
        self.assertIn("pauseBackgroundTabs", code)

if __name__ == "__main__":
    unittest.main()
