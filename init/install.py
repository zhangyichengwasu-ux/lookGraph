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
SKILLS_DIR = Path.home() / ".claude" / "skills"
SETTINGS_FILE = Path.home() / ".claude" / "settings.json"
SCRIPT_DIR = Path(__file__).parent.resolve()
# Hook scripts are in the sibling 'hook' directory
HOOK_SOURCE_DIR = SCRIPT_DIR.parent / "hook"
# Skill files are in the 'skills' subdirectory
SKILL_SOURCE_DIR = SCRIPT_DIR / "skills"

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

def create_skills_directory():
    """Create the skills directory if it doesn't exist"""
    SKILLS_DIR.mkdir(parents=True, exist_ok=True)
    print(f"✓ Created skills directory: {SKILLS_DIR}")

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

    # Copy shared utility libraries
    for lib_file in ["file_hash.py"]:
        src = HOOK_SOURCE_DIR / lib_file
        if src.exists():
            shutil.copy2(src, HOOKS_DIR / lib_file)
            os.chmod(HOOKS_DIR / lib_file, 0o755)
            print(f"✓ Copied: {lib_file}")
        else:
            print(f"✗ Missing: {lib_file}", file=sys.stderr)
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

def install_skills(install_mode="global"):
    """Install LookGraph mode toggle skills

    Args:
        install_mode: "global" to install to ~/.claude/skills/ (available in all projects)
                      "project" to install to <project>/.claude/skills/ (current project only)
    """
    if install_mode == "global":
        # Install to global skills directory (available in all projects)
        skills_base_dir = Path.home() / ".claude" / "skills"
        location_label = "global (~/.claude/skills/)"
    else:
        # Install to project skills directory (current project only)
        project_root = Path(__file__).parent.parent
        skills_base_dir = project_root / ".claude" / "skills"
        location_label = f"project ({skills_base_dir.relative_to(project_root)}/)"

    # Create skills directory if it doesn't exist
    skills_base_dir.mkdir(parents=True, exist_ok=True)

    skills = [
        {
            "name": "look_graph",
            "content": """---
name: look_graph
description: Enter LookGraph mode - prioritize using LookGraph API for all code analysis, search, and context loading
---

# LookGraph Mode

You are now in **LookGraph Mode**.

## Core Behavior Changes

When in LookGraph mode, you MUST follow these priorities:

### 1. Code Search & Discovery
- ❌ **DON'T** use `Grep` or `Glob` directly for code search
- ✅ **DO** use `semantic_search.py` to find code by business intent
- ✅ **DO** use `project_summary.py` to understand overall structure first

### 2. Reading Code Context
- ❌ **DON'T** use `Read` tool directly to read source files
- ✅ **DO** use `class_context.py` to get class overview with relationships
- ✅ **DO** use `method_context.py` to get method details with call chains

### 3. Understanding Dependencies
- ❌ **DON'T** manually trace imports and calls
- ✅ **DO** use `class_relations.py` to see dependencies
- ✅ **DO** use `method_callchain.py` to see call relationships

### 4. Impact Analysis
- ❌ **DON'T** manually search for usages
- ✅ **DO** use `impact_analysis.py` before modifying code

## Workflow

### Starting Analysis
```bash
# 1. Check if project is initialized
python3 ~/.claude/hooks/lookgraph/project_list.py

# 2. If not initialized, init first
python3 ~/.claude/hooks/lookgraph/project_init.py /path/to/project ProjectName

# 3. Get project overview
python3 ~/.claude/hooks/lookgraph/project_summary.py <project_id>
```

### Finding Code
```bash
# Use semantic search instead of grep
python3 ~/.claude/hooks/lookgraph/semantic_search.py <project_id> "business description" 10
```

### Understanding Code
```bash
# Get class context instead of reading file
python3 ~/.claude/hooks/lookgraph/class_context.py <class_id>

# Get method details with dependencies
python3 ~/.claude/hooks/lookgraph/method_context.py <method_id>
```

### Saving Knowledge
After understanding code, create semantic annotations:
```bash
python3 ~/.claude/hooks/lookgraph/semantic_annotate.py \\
  --project-id <project_id> \\
  --project-path /path/to/project \\
  --package com.example \\
  --class ClassName \\
  --type CLASS \\
  --content "Business meaning of this class" \\
  --source AI
```

## When to Fall Back to Traditional Tools

You MAY use `Read`, `Grep`, `Glob` ONLY when:
- Reading configuration files (JSON, YAML, properties)
- Reading documentation (README, markdown)
- Searching for non-code patterns (log messages, comments)
- The project is not initialized in LookGraph

## Current Project Context

**IMPORTANT**: At the start of each conversation in LookGraph mode:
1. Run `project_list.py` to find the current project
2. If found, save the `projectId` in memory
3. Run `project_summary.py` to understand the codebase
4. Keep `projectId` available for all subsequent operations

## Exit

To exit LookGraph mode, user should run: `/exit_look_graph`

---

**LookGraph Mode is now ACTIVE** 🔍
"""
        },
        {
            "name": "exit_look_graph",
            "content": """---
name: exit_look_graph
description: Exit LookGraph mode and return to default file-based code analysis
---

# Exit LookGraph Mode

You have exited **LookGraph Mode**.

## Behavior Restored

You can now use standard tools freely:
- ✅ `Grep` - search code with regex patterns
- ✅ `Glob` - find files by pattern
- ✅ `Read` - read source files directly
- ✅ Manual code tracing and analysis

LookGraph tools are still available if explicitly requested by the user, but they are no longer the default priority.

## Re-entering LookGraph Mode

To re-enter LookGraph mode, user should run: `/look_graph`

---

**LookGraph Mode is now INACTIVE** ⚪
"""
        }
    ]

    for skill in skills:
        # Create skill directory: skills/<skill_name>/
        skill_dir = skills_base_dir / skill["name"]
        skill_dir.mkdir(exist_ok=True)

        # Write SKILL.md file
        skill_file = skill_dir / "SKILL.md"
        with open(skill_file, 'w') as f:
            f.write(skill["content"])
        print(f"✓ Installed skill: {skill['name']} → {skill_dir}/SKILL.md")

    print(f"✓ Skills installed to {location_label}")
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
    # Parse command line arguments
    import argparse
    parser = argparse.ArgumentParser(
        description="LookGraph Installation Script",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Installation modes:
  global  - Install skills to ~/.claude/skills/ (available in all projects)
  project - Install skills to <project>/.claude/skills/ (current project only)

Default is 'global' for convenience.
"""
    )
    parser.add_argument(
        '--mode',
        choices=['global', 'project'],
        default='global',
        help='Installation mode (default: global)'
    )
    args = parser.parse_args()

    print("=" * 60)
    print("LookGraph Hooks Installation")
    print(f"Mode: {args.mode.upper()}")
    print("=" * 60)
    print()

    # Step 1: Create hooks directory
    print("Step 1: Creating hooks directory...")
    create_hooks_directory()
    print()

    # Step 2: Create skills directory (deprecated, but keep for compatibility)
    print("Step 2: Creating skills directory...")
    create_skills_directory()
    print()

    # Step 3: Copy hook scripts
    print("Step 3: Copying hook scripts...")
    if not copy_hook_scripts():
        print("\n✗ Installation failed: Some hook scripts are missing")
        sys.exit(1)
    print()

    # Step 4: Install skills
    print(f"Step 4: Installing LookGraph mode toggle skills ({args.mode})...")
    if not install_skills(install_mode=args.mode):
        print("\n✗ Installation failed: Could not install skills")
        sys.exit(1)
    print()

    # Step 5: Register hooks
    print("Step 5: Creating CLAUDE.md documentation...")
    if not register_hooks():
        print("\n✗ Installation failed: Could not create CLAUDE.md")
        sys.exit(1)
    print()

    # Step 6: Generate README
    print("Step 6: Generating README...")
    generate_readme()
    print()

    print("=" * 60)
    print("✓ Installation completed successfully!")
    print("=" * 60)
    print()
    print("Hooks installed to:", HOOKS_DIR)
    if args.mode == "global":
        print("Skills installed to: ~/.claude/skills/ (global - all projects)")
    else:
        project_root = Path(__file__).parent.parent
        print(f"Skills installed to: {project_root}/.claude/skills/ (project only)")
    print("Documentation created:", Path.home() / ".claude" / "CLAUDE.md")
    print()
    print("✓ Claude can now recognize and use LookGraph tools!")
    print()
    print("Available commands:")
    print("  /look_graph       - Enter LookGraph mode (prioritize LookGraph API)")
    print("  /exit_look_graph  - Exit LookGraph mode (use standard tools)")
    print()
    print("Example usage:")
    print("  1. Restart Claude Code")
    print("  2. Type: /look_graph")
    print("  3. Ask Claude: 'Initialize this project with lookgraph'")
    print("  4. Ask Claude: 'Find the user login logic'")
    print()
    if args.mode == "global":
        print("✓ Skills are available in ALL projects after restart")
    else:
        print("⚠ Skills are only available in this project")
        print("  To install globally: python3 install.py --mode global")
    print()
    print("Note: Make sure the LookGraph server is running at http://localhost:8090")
    print("      Start it with: cd /path/to/lookGraph && bash run.sh &")

if __name__ == "__main__":
    main()
