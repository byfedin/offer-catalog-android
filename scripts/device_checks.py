#!/usr/bin/env python3
"""Host-side process restoration checks and cold Activity startup measurements."""

import argparse
import json
import re
import statistics
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "dev.byfedin.offercatalog"
ACTIVITY = f"{PACKAGE}/.MainActivity"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit emulator/device serial")
    parser.add_argument("--mode", choices=["process-death", "startup"], required=True)
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.iterations < 3:
        parser.error("Use at least three startup iterations")

    def adb(*words):
        return subprocess.check_output(
            ["adb", "-s", args.serial, *words], text=True, stderr=subprocess.STDOUT, timeout=30
        ).strip()

    def shell(*words):
        return adb("shell", *words)

    def wait_for(predicate, description, timeout=15):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            value = predicate()
            if value:
                return value
            time.sleep(0.2)
        tree = hierarchy()
        args.output.parent.mkdir(parents=True, exist_ok=True)
        ET.ElementTree(tree).write(args.output.with_suffix(".xml"), encoding="utf-8")
        visible = [(n.get("text"), n.get("class")) for n in tree.iter("node") if n.get("text")]
        raise AssertionError(f"Timed out: {description}; visible content: {visible}")

    def hierarchy():
        shell("uiautomator", "dump", "/sdcard/offer-check.xml")
        return ET.fromstring(shell("cat", "/sdcard/offer-check.xml"))

    def find(predicate):
        return next((n for n in hierarchy().iter("node") if predicate(n)), None)

    def tap(node):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
        shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))

    def visible_text(value):
        # Element objects with no children are falsey, so return a Boolean explicitly.
        return find(lambda n: n.get("text") == value) is not None

    def launch():
        return shell(
            "am", "start", "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER", "-f", "0x10200000", "-n", ACTIVITY,
        )

    result = {
        "mode": args.mode,
        "api": shell("getprop", "ro.build.version.sdk"),
        "model": shell("getprop", "ro.product.model"),
        "emulator": shell("getprop", "ro.kernel.qemu") == "1",
        "build_type": "debug",
    }
    if args.mode == "process-death":
        # Force-stop establishes a fresh scenario; it is not the process-death operation.
        shell("am", "force-stop", PACKAGE)
        shell("am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "-f", "0x10008000", "-n", ACTIVITY)
        wait_for(
            lambda: visible_text("Search offers"),
            "search field",
        )
        tap(find(lambda n: n.get("text") == "Search offers"))
        shell("input", "text", "coast")
        shell("input", "keyevent", "4")
        travel = find(lambda n: n.get("text") == "Travel" and n.get("clickable") == "true")
        if travel is None:
            travel = find(lambda n: n.get("text") == "Travel")
        if travel is None:
            raise AssertionError("Travel category is not accessible")
        tap(travel)
        wait_for(lambda: visible_text("Along the coast"), "filtered offer")
        old_pid = shell("pidof", PACKAGE)
        shell("input", "keyevent", "3")
        def is_background():
            activity = shell("dumpsys", "activity", "activities")
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.with_name("activity-before-kill.txt").write_text(activity)
            resumed = [line for line in activity.splitlines() if "ResumedActivity" in line or "mResumedActivity" in line]
            records = [block for block in activity.split("Hist #")[1:] if PACKAGE in block.splitlines()[0]]
            saved = any("state=stopped" in block.lower() and "havestate=true" in block.lower() for block in records)
            return bool(resumed) and all(PACKAGE not in line for line in resumed) and saved
        wait_for(is_background, "stopped activity with saved instance state")
        # run-as is available for this debug APK; SIGKILL preserves the retained task.
        if not old_pid.isdigit():
            raise AssertionError("Expected a single app process")
        shell("run-as", PACKAGE, "kill", "-9", old_pid)
        wait_for(
            lambda: not any(line.split()[-1:] == [PACKAGE] for line in shell("ps", "-A").splitlines()),
            "background process termination",
        )
        launch()
        wait_for(lambda: visible_text("Along the coast"), "restored filtered result")
        tree = hierarchy()
        fields = [n for n in tree.iter("node") if n.get("text") == "coast"]
        assert any(n.get("text") == "coast" for n in fields), "Search query was lost"
        categories = [n for n in tree.iter("node") if n.get("text") == "Travel" or any(c.get("text") == "Travel" for c in n)]
        assert any(n.get("checked") == "true" or n.get("selected") == "true" for n in categories), "Category was lost"
        assert shell("pidof", PACKAGE) != old_pid, "The process was not replaced"
        result.update(passed=True, query="coast", category="Travel", task_preserved=True, process_replaced=True)
    else:
        samples = []
        for _ in range(args.iterations):
            shell("am", "force-stop", PACKAGE)
            output = shell("am", "start", "-W", "-f", "0x10008000", "-n", ACTIVITY)
            times = dict(re.findall(r"^(ThisTime|TotalTime|WaitTime):\s*(\d+)$", output, re.MULTILINE))
            if "TotalTime" not in times:
                raise AssertionError(f"Missing Activity timing: {output}")
            samples.append(int(times["TotalTime"]))
            wait_for(lambda: visible_text("The art of noticing"), "first catalog result")
        result.update(
            metric="am start -W TotalTime (ms)", samples_ms=samples,
            median_ms=statistics.median(samples), min_ms=min(samples), max_ms=max(samples),
            scope="Cold process, warm filesystem; Activity display, not catalog readiness. No speed threshold.",
        )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
