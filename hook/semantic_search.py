#!/usr/bin/env python3
"""
Hook: Semantic search in code
Usage: python semantic_search.py <project_id> <query> [top_k]
"""

import sys
from lookgraph_client import get_client

def main():
    if len(sys.argv) < 3:
        print("Usage: python semantic_search.py <project_id> <query> [top_k]", file=sys.stderr)
        sys.exit(1)

    project_id = sys.argv[1]
    query = sys.argv[2]
    top_k = int(sys.argv[3]) if len(sys.argv) > 3 else 5

    client = get_client()

    request_data = {
        "projectId": project_id,
        "query": query,
        "topK": top_k
    }

    response = client.post("/api/semantic/search", data=request_data)
    print(client.format_output(response))

if __name__ == "__main__":
    main()
