#!/usr/bin/env python3
"""
LookGraph Hooks Installation Script
Installs LookGraph hook scripts to ~/.claude/hooks and registers them in settings.json
"""

import os
import sys
import json
import shutil
from pathlib import Path

HOOKS_DIR = Path.home() / ".claude" / "hooks" / "lookgraph"
SETTINGS_FILE = Path.home() / ".claude" / "settings.json"
SCRIPT_DIR = Path(__file__).parent.resolve()

# Define all hooks with their metadata
HOOKS = [
    {
        "name": "lookgraph:project-list",
        "file": "project_list.py",
        "description": "Get all projects in LookGraph",
        "usage": "lookgraph:project-list",
        "command": "python3 ~/.claude/hooks/lookgraph/project_list.py"
    },
    {
        "name": "lookgraph:project-init",
        "file": "project_init.py",
        "description": "Initialize a new project in LookGraph",
        "usage": "lookgraph:project-init <project_path> <project_name>",
        "command": "python3 ~/.claude/hooks/lookgraph/project_init.py"
    },
    {
        "name": "lookgraph:project-update",
        "file": "project_update.py",
        "description": "Trigger incremental update for a project",
        "usage": "lookgraph:project-update <project_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/project_update.py"
    },
    {
        "name": "lookgraph:project-summary",
        "file": "project_summary.py",
        "description": "Get project summary",
        "usage": "lookgraph:project-summary <project_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/project_summary.py"
    },
    {
        "name": "lookgraph:semantic-search",
        "file": "semantic_search.py",
        "description": "Search code semantically",
        "usage": "lookgraph:semantic-search <project_id> <query> [top_k]",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_search.py"
    },
    {
        "name": "lookgraph:semantic-by-git",
        "file": "semantic_by_git.py",
        "description": "Get semantic annotations by git commit hash",
        "usage": "lookgraph:semantic-by-git <git_commit_hash>",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_by_git.py"
    },
    {
        "name": "lookgraph:semantic-by-node",
        "file": "semantic_by_node.py",
        "description": "Get semantic annotations by Neo4j node ID",
        "usage": "lookgraph:semantic-by-node <neo4j_node_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_by_node.py"
    },
    {
        "name": "lookgraph:semantic-class-history",
        "file": "semantic_class_history.py",
        "description": "Get class semantic annotation history",
        "usage": "lookgraph:semantic-class-history <package_name> <class_name>",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_class_history.py"
    },
    {
        "name": "lookgraph:semantic-method-history",
        "file": "semantic_method_history.py",
        "description": "Get method semantic annotation history",
        "usage": "lookgraph:semantic-method-history <package_name> <class_name> <method_name>",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_method_history.py"
    },
    {
        "name": "lookgraph:class-relations",
        "file": "class_relations.py",
        "description": "Get class relations",
        "usage": "lookgraph:class-relations <class_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/class_relations.py"
    },
    {
        "name": "lookgraph:class-methods",
        "file": "class_methods.py",
        "description": "Get all methods in a class",
        "usage": "lookgraph:class-methods <class_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/class_methods.py"
    },
    {
        "name": "lookgraph:class-context",
        "file": "class_context.py",
        "description": "Get class context",
        "usage": "lookgraph:class-context <class_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/class_context.py"
    },
    {
        "name": "lookgraph:method-callchain",
        "file": "method_callchain.py",
        "description": "Get method call chain",
        "usage": "lookgraph:method-callchain <method_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/method_callchain.py"
    },
    {
        "name": "lookgraph:method-context",
        "file": "method_context.py",
        "description": "Get method context",
        "usage": "lookgraph:method-context <method_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/method_context.py"
    },
    {
        "name": "lookgraph:module-classes",
        "file": "module_classes.py",
        "description": "Get all classes in a module",
        "usage": "lookgraph:module-classes <module_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/module_classes.py"
    },
    {
        "name": "lookgraph:impact-analysis",
        "file": "impact_analysis.py",
        "description": "Get impact analysis for code changes",
        "usage": "lookgraph:impact-analysis <entity_type> <entity_id>",
        "command": "python3 ~/.claude/hooks/lookgraph/impact_analysis.py"
    }
]

def create_hooks_directory():
    """Create the hooks directory if it doesn't exist"""
    HOOKS_DIR.mkdir(parents=True, exist_ok=True)
    print(f"✓ Created hooks directory: {HOOKS_DIR}")

def copy_hook_scripts():
    """Copy all hook scripts to the hooks directory"""
    copied = []

    # Copy the client library
    client_file = SCRIPT_DIR / "lookgraph_client.py"
    if client_file.exists():
        shutil.copy2(client_file, HOOKS_DIR / "lookgraph_client.py")
        print(f"✓ Copied: lookgraph_client.py")
    else:
        print(f"✗ Missing: lookgraph_client.py", file=sys.stderr)
        return False

    # Copy all hook scripts
    for hook in HOOKS:
        src = SCRIPT_DIR / hook["file"]
        dst = HOOKS_DIR / hook["file"]

        if src.exists():
            shutil.copy2(src, dst)
            # Make executable
            os.chmod(dst, 0o755)
            copied.append(hook["file"])
            print(f"✓ Copied: {hook['file']}")
        else:
            print(f"✗ Missing: {hook['file']}", file=sys.stderr)
            return False

    return True

def load_settings():
    """Load existing settings.json or create a new one"""
    if SETTINGS_FILE.exists():
        with open(SETTINGS_FILE, 'r') as f:
            return json.load(f)
    else:
        # Create .claude directory if it doesn't exist
        SETTINGS_FILE.parent.mkdir(parents=True, exist_ok=True)
        return {}

def save_settings(settings):
    """Save settings.json"""
    with open(SETTINGS_FILE, 'w') as f:
        json.dump(settings, f, indent=2)
    print(f"✓ Updated settings: {SETTINGS_FILE}")

def register_hooks():
    """Register hooks in settings.json"""
    settings = load_settings()

    # Initialize hooks section if it doesn't exist
    if "hooks" not in settings:
        settings["hooks"] = {}

    # Add LookGraph hooks
    # Note: In Claude Code, hooks are typically event-based (e.g., pre-commit, post-read)
    # For custom commands that can be invoked manually, we use a custom namespace

    # Since Claude Code doesn't have a native way to register custom commands,
    # we'll document them in a custom section for reference
    if "customCommands" not in settings:
        settings["customCommands"] = {}

    settings["customCommands"]["lookgraph"] = {
        "description": "LookGraph code analysis and semantic search tools",
        "baseUrl": "http://localhost:8080",
        "commands": []
    }

    for hook in HOOKS:
        settings["customCommands"]["lookgraph"]["commands"].append({
            "name": hook["name"],
            "description": hook["description"],
            "usage": hook["usage"],
            "command": hook["command"]
        })

    save_settings(settings)
    return True

def generate_readme():
    """Generate a README for the hooks"""
    readme_content = """# LookGraph Claude Code Hooks

This directory contains Python scripts for interacting with the LookGraph API from Claude Code.

## Available Commands

"""

    for hook in HOOKS:
        readme_content += f"### {hook['name']}\n"
        readme_content += f"{hook['description']}\n\n"
        readme_content += f"**Usage:** `{hook['usage']}`\n\n"
        readme_content += f"**Command:** `{hook['command']}`\n\n"

    readme_content += """
## Configuration

The base URL for the LookGraph API is set in `lookgraph_client.py` (default: http://localhost:8080).

You can override it by setting the `LOOKGRAPH_BASE_URL` environment variable.

## Usage in Claude Code

These scripts can be invoked from Claude Code hooks or called directly from the command line.

Example:
```bash
python3 ~/.claude/hooks/lookgraph/project_list.py
```

## Requirements

- Python 3.6+
- requests library: `pip install requests`
"""

    readme_path = HOOKS_DIR / "README.md"
    with open(readme_path, 'w') as f:
        f.write(readme_content)

    print(f"✓ Generated README: {readme_path}")

def main():
    print("=" * 60)
    print("LookGraph Hooks Installation")
    print("=" * 60)
    print()

    # Step 1: Create hooks directory
    print("Step 1: Creating hooks directory...")
    create_hooks_directory()
    print()

    # Step 2: Copy hook scripts
    print("Step 2: Copying hook scripts...")
    if not copy_hook_scripts():
        print("\n✗ Installation failed: Some hook scripts are missing")
        sys.exit(1)
    print()

    # Step 3: Register hooks
    print("Step 3: Registering hooks in settings.json...")
    if not register_hooks():
        print("\n✗ Installation failed: Could not register hooks")
        sys.exit(1)
    print()

    # Step 4: Generate README
    print("Step 4: Generating README...")
    generate_readme()
    print()

    print("=" * 60)
    print("✓ Installation completed successfully!")
    print("=" * 60)
    print()
    print("Hooks installed to:", HOOKS_DIR)
    print("Settings updated:", SETTINGS_FILE)
    print()
    print("You can now use LookGraph hooks in Claude Code.")
    print()
    print("Example usage:")
    print("  python3 ~/.claude/hooks/lookgraph/project_list.py")
    print()
    print("Note: Make sure the LookGraph server is running at http://localhost:8080")
    print("      and you have the 'requests' library installed: pip install requests")

if __name__ == "__main__":
    main()
