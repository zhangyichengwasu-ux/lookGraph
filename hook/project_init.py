#!/usr/bin/env python3
"""
Hook: Initialize project map
Usage: python project_init.py <project_path> <project_name>
"""

import sys
import json
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 3:
        print("Usage: python project_init.py <project_path> <project_name>", file=sys.stderr)
        sys.exit(1)

    project_path = sys.argv[1]
    project_name = sys.argv[2]

    client = get_client()

    request_data = {
        "path": project_path,
        "name": project_name,
        "language": "JAVA"
    }

    response = client.post("/api/project/init", data=request_data)
    print(client.format_output(response))

if __name__ == "__main__":
    main()
