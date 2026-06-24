#!/usr/bin/env python3
"""
Hook: Get method semantic annotation history
Usage: python semantic_method_history.py <package_name> <class_name> <method_name>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 4:
        print("Usage: python semantic_method_history.py <package_name> <class_name> <method_name>", file=sys.stderr)
        sys.exit(1)

    package_name = sys.argv[1]
    class_name = sys.argv[2]
    method_name = sys.argv[3]

    client = get_client()
    response = client.get("/api/semantic/method", params={
        "packageName": package_name,
        "className": class_name,
        "methodName": method_name
    })
    print(client.format_output(response))

if __name__ == "__main__":
    main()
