import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

class TestStorageAndHome(unittest.TestCase):
    def test_files_exist(self):
        home_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "home")
        dl_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "downloads")
        db_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "db")
        feat_pkg = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "features")
        res_layout = os.path.join(REPO_ROOT, "app", "src", "main", "res", "layout")
        res_menu = os.path.join(REPO_ROOT, "app", "src", "main", "res", "menu")

        files = [
            os.path.join(home_pkg, "HomePageManager.kt"),
            os.path.join(dl_pkg, "NativeDownloadHandler.kt"),
            os.path.join(dl_pkg, "DownloadsBottomSheet.kt"),
            os.path.join(db_pkg, "BookmarkDatabase.kt"),
            os.path.join(feat_pkg, "DesktopModeManager.kt"),
            os.path.join(feat_pkg, "IncognitoManager.kt"),
            os.path.join(res_layout, "layout_home_page.xml"),
            os.path.join(res_layout, "bottom_sheet_downloads.xml"),
            os.path.join(res_menu, "main_menu.xml")
        ]
        for f in files:
            self.assertTrue(os.path.exists(f), f"Missing {f}")

    def test_database_and_download_contracts(self):
        db_path = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "db", "BookmarkDatabase.kt")
        dl_path = os.path.join(REPO_ROOT, "app", "src", "main", "java", "com", "antigravity", "oberon", "downloads", "NativeDownloadHandler.kt")
        menu_path = os.path.join(REPO_ROOT, "app", "src", "main", "res", "menu", "main_menu.xml")

        if not os.path.exists(db_path) or not os.path.exists(dl_path):
            self.fail("Files not created yet")

        with open(db_path, "r", encoding="utf-8") as f:
            db_code = f.read()
        self.assertIn("SQLiteOpenHelper", db_code)
        self.assertIn("fun addBookmark(", db_code)
        self.assertIn("fun addHistory(", db_code)
        self.assertIn("fun getTopVisited(", db_code)

        with open(dl_path, "r", encoding="utf-8") as f:
            dl_code = f.read()
        self.assertIn("DownloadManager", dl_code)
        self.assertIn("enqueue", dl_code)

        with open(menu_path, "r", encoding="utf-8") as f:
            menu_xml = f.read()
        self.assertIn("menu_new_tab", menu_xml)
        self.assertIn("menu_desktop_site", menu_xml)
        self.assertIn("menu_downloads", menu_xml)

if __name__ == "__main__":
    unittest.main()
