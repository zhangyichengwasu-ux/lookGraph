#!/usr/bin/env python3
"""
Hook: Get all classes in a module
Usage: python module_classes.py <module_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python module_classes.py <module_id>", file=sys.stderr)
        sys.exit(1)

    module_id = sys.argv[1]

    client = get_client()
    response = client.get(f"/api/structure/module/{module_id}/classes")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
