#!/usr/bin/env python3
"""
Hook: Get impact analysis for code changes
Usage: python impact_analysis.py <entity_type> <entity_id>
       entity_type: CLASS, METHOD, MODULE
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 3:
        print("Usage: python impact_analysis.py <entity_type> <entity_id>", file=sys.stderr)
        print("       entity_type: CLASS, METHOD, MODULE", file=sys.stderr)
        sys.exit(1)

    entity_type = sys.argv[1]
    entity_id = sys.argv[2]

    client = get_client()
    response = client.post("/api/structure/impact", data={
        "entityType": entity_type,
        "entityId": entity_id
    })
    print(client.format_output(response))

if __name__ == "__main__":
    main()
