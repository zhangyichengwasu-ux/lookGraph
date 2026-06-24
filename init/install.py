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
# Hook scripts are in the sibling 'hook' directory
HOOK_SOURCE_DIR = SCRIPT_DIR.parent / "hook"

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
    },
    {
        "name": "lookgraph:semantic-annotate",
        "file": "semantic_annotate.py",
        "description": "Create or update semantic annotations",
        "usage": "lookgraph:semantic-annotate --project-id ID --package PKG --class CLASS --type TYPE --content CONTENT --source AI|HUMAN",
        "command": "python3 ~/.claude/hooks/lookgraph/semantic_annotate.py"
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
    client_file = HOOK_SOURCE_DIR / "lookgraph_client.py"
    if client_file.exists():
        shutil.copy2(client_file, HOOKS_DIR / "lookgraph_client.py")
        print(f"✓ Copied: lookgraph_client.py")
    else:
        print(f"✗ Missing: lookgraph_client.py", file=sys.stderr)
        return False

    # Copy all hook scripts
    for hook in HOOKS:
        src = HOOK_SOURCE_DIR / hook["file"]
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
    """Create CLAUDE.md to make Claude aware of LookGraph tools"""
    claude_md_path = Path.home() / ".claude" / "CLAUDE.md"
    template_path = Path(__file__).parent / "CLAUDE_MD_TEMPLATE.md"

    # Read the template file
    if template_path.exists():
        with open(template_path, 'r') as f:
            claude_md_content = f.read()
    else:
        # Fallback to simple version if template not found
        claude_md_content = """# LookGraph 代码分析工具

## 简介

LookGraph 是一个代码分析和语义搜索系统，Hook 脚本已安装在 `~/.claude/hooks/lookgraph/`。

## 快速开始

```bash
# 初始化项目
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project ProjectName

# 列出所有项目
python3 ~/.claude/hooks/lookgraph/project_list.py

# 获取项目摘要
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>

# 语义搜索
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "查询内容" 5
```

## 配置

- **API 地址**: http://localhost:8090 (默认)
- **环境变量**: 可设置 `LOOKGRAPH_BASE_URL` 覆盖默认地址

详细文档见 `~/.claude/hooks/lookgraph/README.md`
"""

    with open(claude_md_path, 'w') as f:
        f.write(claude_md_content)

    print(f"✓ Created CLAUDE.md: {claude_md_path}")
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
    print("Step 3: Creating CLAUDE.md documentation...")
    if not register_hooks():
        print("\n✗ Installation failed: Could not create CLAUDE.md")
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
    print("Documentation created:", Path.home() / ".claude" / "CLAUDE.md")
    print()
    print("✓ Claude can now recognize and use LookGraph tools!")
    print()
    print("Example usage:")
    print("  Ask Claude: 'Use lookgraph to list all projects'")
    print("  Ask Claude: 'Initialize this project with lookgraph'")
    print()
    print("Note: Make sure the LookGraph server is running at http://localhost:8090")
    print("      Start it with: cd /path/to/lookGraph && bash run.sh &")

if __name__ == "__main__":
    main()
