#!/usr/bin/env python3
"""
Hook: Get method call chain
Usage: python method_callchain.py <method_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python method_callchain.py <method_id>", file=sys.stderr)
        sys.exit(1)

    method_id = sys.argv[1]

    client = get_client()
    response = client.post("/api/structure/method/callchain", data={"methodId": method_id})
    print(client.format_output(response))

if __name__ == "__main__":
    main()
