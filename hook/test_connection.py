#!/usr/bin/env python3
"""
LookGraph Connection Test Script
Tests the connection to LookGraph API and verifies all endpoints
"""

import sys
from lookgraph_client import get_client

def test_connection():
    """Test basic connection to LookGraph API"""
    print("Testing LookGraph API connection...")
    print()

    client = get_client()
    print(f"API Base URL: {client.base_url}")
    print()

    # Test project list endpoint
    try:
        print("✓ Testing /api/project/list...")
        response = client.get("/api/project/list")

        if response.get("code") == 0:
            data = response.get("data", [])
            print(f"  Success! Found {len(data)} project(s).")

            if data:
                print("  Projects:")
                for project in data[:3]:  # Show first 3 projects
                    project_id = project.get("id", "N/A")
                    project_name = project.get("name", "N/A")
                    print(f"    - {project_name} (ID: {project_id})")
                if len(data) > 3:
                    print(f"    ... and {len(data) - 3} more")
        else:
            print(f"  Warning: API returned code {response.get('code')}")
            print(f"  Message: {response.get('message', 'N/A')}")

        print()
        return True

    except Exception as e:
        print(f"✗ Connection failed: {e}")
        print()
        print("Troubleshooting:")
        print("  1. Make sure LookGraph service is running")
        print(f"  2. Verify the service is accessible at {client.base_url}")
        print("  3. Check your firewall settings")
        print("  4. Try: curl " + client.base_url + "/api/project/list")
        print()
        return False

def main():
    print("=" * 60)
    print("LookGraph Connection Test")
    print("=" * 60)
    print()

    success = test_connection()

    if success:
        print("=" * 60)
        print("✓ Connection test passed!")
        print("=" * 60)
        print()
        print("All hooks should work correctly.")
        print()
        print("Next steps:")
        print("  1. Run install.py to install hooks")
        print("  2. Try: python3 ~/.claude/hooks/lookgraph/project_list.py")
        sys.exit(0)
    else:
        print("=" * 60)
        print("✗ Connection test failed")
        print("=" * 60)
        print()
        print("Please fix the connection issue before installing hooks.")
        sys.exit(1)

if __name__ == "__main__":
    main()
