#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

API = "https://api.github.com"
UPLOAD = "https://uploads.github.com"
TAG = "v1.00"
VERSION = "1.00"
JAR = "release/ItemCooldowns-1.00.jar"
CHANGELOG = (
    "### v1.00 — initial release\n\n"
    "- Cooldowns for **mace** (45s), **spear** (5s, all 7 variants, 1.21.11+), **trident** (5s), **end crystal** (45s) and **respawn anchor** (45s)\n"
    "- Ender pearl style cooldown overlay on inventory slots\n"
    "- Chat messages with remaining seconds\n"
    "- LuckPerms integration, bypass permissions NOT granted by default (even to operators)\n"
    "- Fully configurable config.yml: durations, cancel-action, overlay, messages, pvp-only, world whitelist\n"
    "- Localization: `locale: en` by default, English and Russian bundled, custom languages supported\n"
    "- Works on Paper, Spigot, Bukkit, Purpur, Pufferfish, Leaves and other Bukkit forks\n"
    "- Minecraft 1.21 – 26.x, Folia compatible"
)


def fail(message):
    print(f"[ERROR] {message}", file=sys.stderr)
    sys.exit(1)


def gh(token, method, url, payload=None):
    request = urllib.request.Request(url, method=method)
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", "application/vnd.github+json")
    request.add_header("User-Agent", "itemcooldowns-publisher")
    data = None
    if payload is not None:
        data = json.dumps(payload).encode()
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, data=data) as response:
            body = response.read()
            return response.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as error:
        body = error.read().decode(errors="replace")
        return error.code, {"_error": body}


def run(cmd, cwd):
    process = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    if process.returncode != 0:
        fail(f"command failed: {' '.join(cmd)}\n{process.stderr.strip() or process.stdout.strip()}")
    return process.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description="Create GitHub repo for ItemCooldowns and publish v1.00 release")
    parser.add_argument("--token", default=os.environ.get("GITHUB_TOKEN"),
                        help="GitHub personal access token with 'repo' scope, or set GITHUB_TOKEN env var")
    parser.add_argument("--name", default="itemcooldowns", help="repository name (default: itemcooldowns)")
    parser.add_argument("--private", action="store_true", help="create a private repository")
    parser.add_argument("--modrinth", default="https://modrinth.com/plugin/item_cooldowns",
                        help="Modrinth project URL used as repository homepage")
    args = parser.parse_args()

    if not args.token:
        fail("no token: pass --token or set the GITHUB_TOKEN environment variable")

    root = os.path.dirname(os.path.abspath(__file__))
    if not os.path.exists(os.path.join(root, "pom.xml")):
        fail(f"pom.xml not found in {root} — run this script from the project root")

    code, user = gh(args.token, "GET", f"{API}/user")
    if code != 200:
        fail(f"token rejected by GitHub (HTTP {code}): {str(user.get('_error', ''))[:300]}")
    login = user["login"]
    print(f"[OK] authenticated as {login}")

    repo = f"{login}/{args.name}"
    code, created = gh(args.token, "POST", f"{API}/user/repos", {
        "name": args.name,
        "description": "PvP cooldowns for mace, spear, trident, end crystals and respawn anchors. "
                       "Paper/Spigot/Bukkit plugin, 1.21 - 26.x.",
        "private": args.private,
        "has_issues": True,
        "has_wiki": True,
        "homepage": args.modrinth,
    })
    if code == 201:
        print(f"[OK] repository created: {created['html_url']}")
    elif code == 422 and "already_exists" in str(created.get("_error", "")):
        print(f"[..] repository already exists, reusing: https://github.com/{repo}")
    else:
        fail(f"failed to create repository (HTTP {code}): {str(created.get('_error', ''))[:300]}")

    if subprocess.run(["git", "--version"], capture_output=True).returncode != 0:
        fail("git is not installed")
    if not os.path.exists(os.path.join(root, ".git")):
        init_ok = subprocess.run(["git", "init", "-b", "main"], cwd=root, capture_output=True).returncode == 0
        if not init_ok:
            run(["git", "init"], root)
    run(["git", "config", "user.name", login], root)
    run(["git", "config", "user.email", f"{login}@users.noreply.github.com"], root)
    remote_url = f"https://x-access-token:{args.token}@github.com/{repo}.git"
    if "origin" in run(["git", "remote"], root).split():
        run(["git", "remote", "set-url", "origin", remote_url], root)
    else:
        run(["git", "remote", "add", "origin", remote_url], root)
    run(["git", "add", "-A"], root)
    if run(["git", "status", "--porcelain"], root):
        run(["git", "commit", "-m", f"Initial release {VERSION}"], root)
    if "main" not in run(["git", "branch", "--show-current"], root):
        run(["git", "branch", "-M", "main"], root)
    run(["git", "push", "-u", "origin", "main"], root)
    print("[OK] sources pushed to main")

    if not run(["git", "tag", "-l", TAG], root):
        run(["git", "tag", TAG], root)
    tag_push = subprocess.run(["git", "push", "origin", TAG], cwd=root, capture_output=True, text=True)
    if tag_push.returncode != 0 and "already exists" not in tag_push.stderr:
        fail(f"tag push failed: {tag_push.stderr.strip()[:300]}")
    print(f"[OK] tag {TAG} pushed")

    run(["git", "remote", "set-url", "origin", f"https://github.com/{repo}.git"], root)

    code, release = gh(args.token, "POST", f"{API}/repos/{repo}/releases", {
        "tag_name": TAG,
        "name": f"ItemCooldowns {VERSION}",
        "body": CHANGELOG,
        "draft": False,
        "prerelease": False,
    })
    if code == 201:
        print(f"[OK] release created: {release['html_url']}")
    elif code == 422:
        print("[..] release already exists, skipping")
        print(f"Release page: https://github.com/{repo}/releases/tag/{TAG}")
        return
    else:
        fail(f"failed to create release (HTTP {code}): {str(release.get('_error', ''))[:300]}")

    jar_path = os.path.join(root, JAR)
    if not os.path.exists(jar_path):
        fail(f"jar not found: {jar_path} — build it first with: mvn -DskipTests package")
    with open(jar_path, "rb") as handle:
        jar_bytes = handle.read()
    upload_url = f"{UPLOAD}/repos/{repo}/releases/{release['id']}/assets?name={os.path.basename(jar_path)}"
    request = urllib.request.Request(upload_url, method="POST", data=jar_bytes)
    request.add_header("Authorization", f"Bearer {args.token}")
    request.add_header("Accept", "application/vnd.github+json")
    request.add_header("Content-Type", "application/java-archive")
    request.add_header("User-Agent", "itemcooldowns-publisher")
    try:
        with urllib.request.urlopen(request) as response:
            asset = json.loads(response.read())
            print(f"[OK] jar uploaded: {asset['browser_download_url']}")
    except urllib.error.HTTPError as error:
        fail(f"jar upload failed (HTTP {error.code}): {error.read().decode(errors='replace')[:300]}")

    print()
    print(f"Repository: https://github.com/{repo}")
    print(f"Release:    https://github.com/{repo}/releases/tag/{TAG}")
    print("Put these links into Modrinth -> Edit -> External links (Source / Issues).")


if __name__ == "__main__":
    main()
