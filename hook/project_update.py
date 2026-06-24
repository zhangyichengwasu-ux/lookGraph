#!/usr/bin/env python3
"""
Hook: Trigger incremental project update
Usage: python project_update.py <project_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python project_update.py <project_id>", file=sys.stderr)
        sys.exit(1)

    project_id = sys.argv[1]

    client = get_client()
    response = client.post("/api/project/update", params={"projectId": project_id})
    print(client.format_output(response))

if __name__ == "__main__":
    main()
