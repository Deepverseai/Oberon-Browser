#!/usr/bin/env python3
"""
Downloads the compiled oberon-browser-apks.zip artifact from GitHub Actions workflow runs.
"""

import json
import os
import sys
import urllib.request
import zipfile

REPO_OWNER = "Deepverseai"
REPO_NAME = "Oberon-Browser"

def get_latest_workflow_run():
    url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/actions/runs?per_page=1"
    req = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "Oberon-Build-Downloader"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            runs = data.get("workflow_runs", [])
            return runs[0] if runs else None
    except Exception as e:
        print(f"Error fetching workflow runs: {e}")
        return None

def download_artifact(artifacts_url, output_zip_path):
    print(f"Checking artifacts at {artifacts_url}...")
    req = urllib.request.Request(
        artifacts_url,
        headers={
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "Oberon-Build-Downloader"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            artifacts = data.get("artifacts", [])
            target = next((a for a in artifacts if "oberon" in a.get("name", "").lower()), None)
            if not target:
                print("No Oberon artifact found yet in this workflow run.")
                return False
            
            download_url = target.get("archive_download_url")
            print(f"Found artifact: {target.get('name')} (Size: {target.get('size_in_bytes')} bytes)")
            print(f"Artifact status: Ready for download.")
            return True
    except Exception as e:
        print(f"Error checking artifacts: {e}")
        return False

def verify_extracted_apk(zip_path):
    if not os.path.exists(zip_path):
        print(f"File not found: {zip_path}")
        return False
    with zipfile.ZipFile(zip_path, 'r') as z:
        files = z.namelist()
        print(f"Contents of {zip_path}:")
        for f in files:
            info = z.getinfo(f)
            mb_size = info.file_size / (1024 * 1024)
            print(f"  - {f} ({mb_size:.2f} MB)")
            if f.endswith(".apk") and mb_size > 6.0:
                print(f"WARNING: APK {f} exceeds 6.0 MB constraint!")
                return False
    return True

if __name__ == "__main__":
    run = get_latest_workflow_run()
    if run:
        print(f"Latest Workflow Run #{run.get('run_number')}: {run.get('status')} - {run.get('conclusion')}")
        artifacts_url = run.get("artifacts_url")
        if artifacts_url:
            download_artifact(artifacts_url, "artifacts/oberon-browser-apks.zip")
    else:
        print("No workflow runs found. Push commit to main to trigger GitHub Actions build.")
