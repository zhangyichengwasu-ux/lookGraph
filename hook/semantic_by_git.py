#!/usr/bin/env python3
"""
Hook: Get semantic annotations by git commit hash
Usage: python semantic_by_git.py <git_commit_hash>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python semantic_by_git.py <git_commit_hash>", file=sys.stderr)
        sys.exit(1)

    git_hash = sys.argv[1]

    client = get_client()
    response = client.get(f"/api/semantic/git/{git_hash}")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
