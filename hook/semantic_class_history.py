#!/usr/bin/env python3
"""
Hook: Get class semantic annotation history
Usage: python semantic_class_history.py <package_name> <class_name>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 3:
        print("Usage: python semantic_class_history.py <package_name> <class_name>", file=sys.stderr)
        sys.exit(1)

    package_name = sys.argv[1]
    class_name = sys.argv[2]

    client = get_client()
    response = client.get("/api/semantic/class", params={
        "packageName": package_name,
        "className": class_name
    })
    print(client.format_output(response))

if __name__ == "__main__":
    main()
