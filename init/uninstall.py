#!/usr/bin/env python3
"""
LookGraph Hooks Uninstallation Script
Removes LookGraph hook scripts from ~/.claude/hooks and CLAUDE.md
"""

import os
import sys
import json
import shutil
from pathlib import Path

HOOKS_DIR = Path.home() / ".claude" / "hooks" / "lookgraph"
CLAUDE_MD = Path.home() / ".claude" / "CLAUDE.md"
SETTINGS_FILE = Path.home() / ".claude" / "settings.json"

def remove_hooks_directory():
    """Remove the hooks directory"""
    if HOOKS_DIR.exists():
        shutil.rmtree(HOOKS_DIR)
        print(f"✓ Removed hooks directory: {HOOKS_DIR}")
        return True
    else:
        print(f"⚠ Hooks directory not found: {HOOKS_DIR}")
        return False

def remove_claude_md():
    """Remove CLAUDE.md if it contains LookGraph content"""
    if not CLAUDE_MD.exists():
        print(f"⚠ CLAUDE.md not found: {CLAUDE_MD}")
        return False

    try:
        with open(CLAUDE_MD, 'r') as f:
            content = f.read()

        if 'lookgraph' in content.lower():
            response = input("\nCLAUDE.md contains LookGraph documentation. Remove it? (y/n): ")
            if response.lower() == 'y':
                os.remove(CLAUDE_MD)
                print(f"✓ Removed: {CLAUDE_MD}")
                return True
            else:
                print(f"⚠ Kept: {CLAUDE_MD}")
                return False
        else:
            print(f"⚠ CLAUDE.md exists but doesn't contain LookGraph content")
            return False

    except Exception as e:
        print(f"✗ Error checking CLAUDE.md: {e}", file=sys.stderr)
        return False

def update_settings():
    """Remove LookGraph entries from settings.json"""
    if not SETTINGS_FILE.exists():
        print(f"⚠ Settings file not found: {SETTINGS_FILE}")
        return False

    try:
        with open(SETTINGS_FILE, 'r') as f:
            settings = json.load(f)

        modified = False

        # Remove customCommands.lookgraph (legacy from old install script)
        if "customCommands" in settings and "lookgraph" in settings["customCommands"]:
            del settings["customCommands"]["lookgraph"]
            modified = True
            print("✓ Removed customCommands.lookgraph from settings.json")

            # Remove customCommands section if empty
            if not settings["customCommands"]:
                del settings["customCommands"]

        # Check for LookGraph-related hooks
        if "hooks" in settings:
            lookgraph_hooks = {k: v for k, v in settings["hooks"].items()
                             if isinstance(v, str) and "lookgraph" in v.lower()}
            if lookgraph_hooks:
                print("\n⚠ Found LookGraph-related hooks in settings.json:")
                for hook_name, hook_cmd in lookgraph_hooks.items():
                    print(f"  {hook_name}: {hook_cmd}")

                response = input("\nRemove these hooks? (y/n): ")
                if response.lower() == 'y':
                    for hook_name in lookgraph_hooks.keys():
                        del settings["hooks"][hook_name]
                        modified = True
                    print("✓ Removed LookGraph hooks from settings.json")

        if modified:
            with open(SETTINGS_FILE, 'w') as f:
                json.dump(settings, f, indent=2)
            print(f"✓ Updated settings: {SETTINGS_FILE}")
            return True
        else:
            print("⚠ No LookGraph entries found in settings.json")
            return False

    except Exception as e:
        print(f"✗ Error updating settings.json: {e}", file=sys.stderr)
        return False

def main():
    print("=" * 60)
    print("LookGraph Hooks Uninstallation")
    print("=" * 60)
    print()

    if not HOOKS_DIR.exists() and not CLAUDE_MD.exists() and not SETTINGS_FILE.exists():
        print("✓ LookGraph hooks are not installed. Nothing to do.")
        return

    print("This will remove:")
    print(f"  - {HOOKS_DIR}")
    print(f"  - {CLAUDE_MD} (if contains LookGraph content)")
    print(f"  - LookGraph entries from {SETTINGS_FILE}")
    print()

    response = input("Continue? (y/n): ")
    if response.lower() != 'y':
        print("Aborted.")
        return

    print()

    # Step 1: Remove hooks directory
    print("Step 1: Removing hooks directory...")
    remove_hooks_directory()
    print()

    # Step 2: Remove CLAUDE.md
    print("Step 2: Removing CLAUDE.md...")
    remove_claude_md()
    print()

    # Step 3: Clean up settings.json (remove old customCommands if exists)
    print("Step 3: Cleaning settings.json...")
    update_settings()
    print()

    print("=" * 60)
    print("✓ Uninstallation completed!")
    print("=" * 60)

if __name__ == "__main__":
    main()
