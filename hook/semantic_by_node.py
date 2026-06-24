#!/usr/bin/env python3
"""
Hook: Get semantic annotations by Neo4j node ID
Usage: python semantic_by_node.py <neo4j_node_id>
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 2:
        print("Usage: python semantic_by_node.py <neo4j_node_id>", file=sys.stderr)
        sys.exit(1)

    node_id = sys.argv[1]

    client = get_client()
    response = client.get(f"/api/semantic/node/{node_id}")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
