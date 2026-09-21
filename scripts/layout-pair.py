#!/usr/bin/env python3
"""Arrange Chrome and YouTube on an existing FoldPatch virtual display."""
import argparse
import os
from pathlib import Path
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("device", help="Previously paired ADB endpoint")
    parser.add_argument("display", type=int, help="REACHPAD_DISPLAY reported by the bridge")
    parser.add_argument("--width", type=int, default=1414)
    parser.add_argument("--height", type=int, default=2208)
    parser.add_argument("--left-width", type=int, default=707)
    args = parser.parse_args()
    if args.display < 2 or not 0 < args.left_width < args.width or args.height < 200:
        parser.error("Expected a virtual display and valid panel dimensions")
    adb = Path(os.environ.get("ANDROID_SDK_ROOT", str(Path.home() / "Android/Sdk"))) / "platform-tools/adb"

    def shell(*command):
        result = subprocess.run([str(adb), "-s", args.device, "shell", *map(str, command)],
                                check=True, capture_output=True, text=True, timeout=25)
        if "Error:" in result.stdout or "Exception" in result.stdout:
            raise RuntimeError(result.stdout)
        return result.stdout

    # Refuse to modify arbitrary displays supplied by mistake.
    display_info = shell("dumpsys", "display")
    if not any('"FoldPatch"' in line and re.search(rf"displayId[= ]+{args.display}\b", line)
               for line in display_info.splitlines()):
        raise RuntimeError("The requested display is not an active FoldPatch display")

    for package, left, right in [("com.android.chrome", 0, args.left_width),
                                 ("com.google.android.youtube", args.left_width, args.width)]:
        launch = ["-n", "com.android.chrome/com.google.android.apps.chrome.Main"] if package == "com.android.chrome" else [
            "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "-p", package]
        shell("am", "start", "-W", "--display", args.display, "--windowingMode", 5, *launch)
        roots = shell("am", "stack", "list").split("RootTask")
        task = None
        for root in roots:
            if not re.search(rf"displayId={args.display}\b", root) or "mWindowingMode=freeform" not in root:
                continue
            match = re.search(r"taskId=(\d+): " + re.escape(package) + "/", root)
            if match:
                task = int(match[1])
                break
        if task is None:
            raise RuntimeError(f"No freeform task for {package} on the selected display")
        # Samsung enforces a 14 px top margin for these freeform windows.
        shell("am", "task", "resize", task, left, 14, right, args.height)
        print(f"{package}: task {task}, [{left},14]-[{right},{args.height}]")


if __name__ == "__main__":
    main()
