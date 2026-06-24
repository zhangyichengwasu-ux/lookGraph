#!/usr/bin/env python3
"""
Hook: Get all projects
Usage: python project_list.py
"""

import sys
from lookgraph_client import get_client

def main():
    client = get_client()
    response = client.get("/api/project/list")
    print(client.format_output(response))

if __name__ == "__main__":
    main()
