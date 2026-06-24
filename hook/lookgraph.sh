#!/bin/bash
# LookGraph Hook Wrapper Script
# Provides convenient aliases for LookGraph hooks

HOOK_DIR="$HOME/.claude/hooks/lookgraph"

# Check if hooks are installed
if [ ! -d "$HOOK_DIR" ]; then
    echo "Error: LookGraph hooks are not installed."
    echo "Please run: python3 $(dirname "$0")/install.py"
    exit 1
fi

# Function to display usage
show_usage() {
    cat << EOF
LookGraph Hook Commands

Project Management:
  lg-list                          List all projects
  lg-init <path> <name>            Initialize new project
  lg-update <project_id>           Trigger incremental update
  lg-summary <project_id>          Get project summary

Semantic Search:
  lg-search <pid> <query> [top_k]  Search code semantically
  lg-sem-git <hash>                Get annotations by git hash
  lg-sem-node <node_id>            Get annotations by node ID
  lg-sem-class <pkg> <class>       Get class annotation history
  lg-sem-method <pkg> <cls> <mtd>  Get method annotation history

Structure Query:
  lg-class-rel <class_id>          Get class relations
  lg-class-methods <class_id>      Get methods in class
  lg-module-classes <module_id>    Get classes in module
  lg-callchain <method_id>         Get method call chain

Context:
  lg-class-ctx <class_id>          Get class context
  lg-method-ctx <method_id>        Get method context

Impact Analysis:
  lg-impact <type> <id>            Get impact analysis (type: CLASS/METHOD/MODULE)

Environment:
  LOOKGRAPH_BASE_URL               Override API base URL (default: http://localhost:8080)

Examples:
  lg-list
  lg-search proj123 "authentication logic" 10
  lg-impact METHOD method456

EOF
}

# Main command router
case "${1:-help}" in
    # Project commands
    lg-list)
        python3 "$HOOK_DIR/project_list.py"
        ;;
    lg-init)
        python3 "$HOOK_DIR/project_init.py" "${@:2}"
        ;;
    lg-update)
        python3 "$HOOK_DIR/project_update.py" "${@:2}"
        ;;
    lg-summary)
        python3 "$HOOK_DIR/project_summary.py" "${@:2}"
        ;;

    # Semantic commands
    lg-search)
        python3 "$HOOK_DIR/semantic_search.py" "${@:2}"
        ;;
    lg-sem-git)
        python3 "$HOOK_DIR/semantic_by_git.py" "${@:2}"
        ;;
    lg-sem-node)
        python3 "$HOOK_DIR/semantic_by_node.py" "${@:2}"
        ;;
    lg-sem-class)
        python3 "$HOOK_DIR/semantic_class_history.py" "${@:2}"
        ;;
    lg-sem-method)
        python3 "$HOOK_DIR/semantic_method_history.py" "${@:2}"
        ;;

    # Structure commands
    lg-class-rel)
        python3 "$HOOK_DIR/class_relations.py" "${@:2}"
        ;;
    lg-class-methods)
        python3 "$HOOK_DIR/class_methods.py" "${@:2}"
        ;;
    lg-module-classes)
        python3 "$HOOK_DIR/module_classes.py" "${@:2}"
        ;;
    lg-callchain)
        python3 "$HOOK_DIR/method_callchain.py" "${@:2}"
        ;;

    # Context commands
    lg-class-ctx)
        python3 "$HOOK_DIR/class_context.py" "${@:2}"
        ;;
    lg-method-ctx)
        python3 "$HOOK_DIR/method_context.py" "${@:2}"
        ;;

    # Impact analysis
    lg-impact)
        python3 "$HOOK_DIR/impact_analysis.py" "${@:2}"
        ;;

    # Help
    help|--help|-h|*)
        show_usage
        ;;
esac
