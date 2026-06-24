#!/usr/bin/env python3
"""
Hook: Get class relations
Usage: python class_relations.py <class_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python class_relations.py <class_id>", file=sys.stderr)
        sys.exit(1)

    class_id = sys.argv[1]

    client = get_client()
    response = client.get(f"/api/structure/class/{class_id}/relation")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
