#!/usr/bin/env python3
"""
Hook: Get method context
Usage: python method_context.py <method_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python method_context.py <method_id>", file=sys.stderr)
        sys.exit(1)

    method_id = sys.argv[1]

    client = get_client()
    response = client.post("/api/context/method", data={"methodId": method_id})
    print(client.format_output(response))

if __name__ == "__main__":
    main()
