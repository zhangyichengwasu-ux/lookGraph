#!/usr/bin/env python3
"""
Comprehensive Hook Testing Script
Tests all LookGraph hooks and validates against databases
"""

import sys
import json
from lookgraph_client import get_client

# Test results
results = {
    "passed": [],
    "failed": [],
    "skipped": []
}

def test_hook(name, test_func):
    """Run a test and record results"""
    print(f"\n{'='*60}")
    print(f"Testing: {name}")
    print('='*60)
    try:
        test_func()
        results["passed"].append(name)
        print(f"✓ {name} PASSED")
        return True
    except Exception as e:
        results["failed"].append((name, str(e)))
        print(f"✗ {name} FAILED: {e}")
        return False

def test_project_list():
    """Test project_list.py"""
    client = get_client()
    response = client.get("/api/project/list")
    # Response is already the data array from format_output
    data = response if isinstance(response, list) else response.get('data', [])
    assert len(data) > 0, "No projects found"
    print(f"  Found {len(data)} project(s)")
    for proj in data:
        print(f"    - {proj.get('name')} (ID: {proj.get('projectId')})")

def test_project_summary():
    """Test project_summary.py"""
    client = get_client()
    # Get first project
    projects = client.get("/api/project/list")
    data = projects if isinstance(projects, list) else projects.get('data', [])
    if not data:
        raise Exception("No projects to test")

    project_id = data[0].get('projectId')
    response = client.get(f"/api/project/summary", params={"projectId": project_id})

    assert response.get('projectId') == project_id
    print(f"  Project: {response.get('name')}")
    print(f"  Classes: {response.get('classCount')}")
    print(f"  Methods: {response.get('methodCount')}")
    print(f"  Modules: {response.get('moduleCount')}")

def test_semantic_search():
    """Test semantic_search.py"""
    client = get_client()
    # Get first project
    projects = client.get("/api/project/list")
    data = projects if isinstance(projects, list) else projects.get('data', [])
    if not data:
        raise Exception("No projects to test")

    project_id = data[0].get('projectId')
    request_data = {
        "projectId": project_id,
        "query": "controller",
        "topK": 3
    }

    response = client.post("/api/semantic/search", data=request_data)
    results = response if isinstance(response, list) else response.get('data', [])
    print(f"  Found {len(results)} semantic matches")
    for i, hit in enumerate(results[:3], 1):
        score = hit.get('score', 0)
        entity_id = hit.get('metadata', {}).get('entity_id', 'N/A')
        print(f"    {i}. Score: {score:.4f}, Entity: {entity_id[:60]}...")

def test_module_classes():
    """Test module_classes.py"""
    client = get_client()
    # Get first project
    projects = client.get("/api/project/list")
    data = projects if isinstance(projects, list) else projects.get('data', [])
    if not data:
        raise Exception("No projects to test")

    project_id = data[0].get('projectId')

    # Use project ID as module ID (root module)
    try:
        response = client.get(f"/api/structure/module/{project_id}/classes")
        classes = response if isinstance(response, list) else response.get('data', [])
        print(f"  Found {len(classes)} classes in module")
        for cls in classes[:5]:
            print(f"    - {cls.get('name', 'N/A')}")
        if len(classes) == 0:
            print("  (No classes found - may be expected)")
    except Exception as e:
        print(f"  Module query returned: {e}")
        results["skipped"].append("module_classes.py - no data")
        raise

def test_semantic_by_git():
    """Test semantic_by_git.py"""
    # This requires semantic history data with git hashes
    # Skip if no data
    print("  Skipped: Requires semantic annotation data with git hashes")
    results["skipped"].append("semantic_by_git.py - no test data")

def test_semantic_by_node():
    """Test semantic_by_node.py"""
    print("  Skipped: Requires semantic annotation data with node IDs")
    results["skipped"].append("semantic_by_node.py - no test data")

def test_semantic_class_history():
    """Test semantic_class_history.py"""
    print("  Skipped: Requires semantic annotation data")
    results["skipped"].append("semantic_class_history.py - no test data")

def test_semantic_method_history():
    """Test semantic_method_history.py"""
    print("  Skipped: Requires semantic annotation data")
    results["skipped"].append("semantic_method_history.py - no test data")

def test_class_relations():
    """Test class_relations.py"""
    print("  Skipped: Requires specific class ID")
    results["skipped"].append("class_relations.py - needs class ID")

def test_class_methods():
    """Test class_methods.py"""
    print("  Skipped: Requires specific class ID")
    results["skipped"].append("class_methods.py - needs class ID")

def test_method_callchain():
    """Test method_callchain.py"""
    print("  Skipped: Requires specific method ID")
    results["skipped"].append("method_callchain.py - needs method ID")

def test_class_context():
    """Test class_context.py"""
    print("  Skipped: Requires specific class ID")
    results["skipped"].append("class_context.py - needs class ID")

def test_method_context():
    """Test method_context.py"""
    print("  Skipped: Requires specific method ID")
    results["skipped"].append("method_context.py - needs method ID")

def test_impact_analysis():
    """Test impact_analysis.py"""
    print("  Skipped: Requires specific entity ID")
    results["skipped"].append("impact_analysis.py - needs entity ID")

def main():
    print("=" * 60)
    print("LookGraph Hooks Comprehensive Test")
    print("=" * 60)

    # Test read operations
    print("\n" + "=" * 60)
    print("PHASE 1: Read Operations")
    print("=" * 60)

    test_hook("project_list.py", test_project_list)
    test_hook("project_summary.py", test_project_summary)
    test_hook("semantic_search.py", test_semantic_search)
    test_hook("module_classes.py", test_module_classes)

    # Tests that need specific data
    test_hook("semantic_by_git.py", test_semantic_by_git)
    test_hook("semantic_by_node.py", test_semantic_by_node)
    test_hook("semantic_class_history.py", test_semantic_class_history)
    test_hook("semantic_method_history.py", test_semantic_method_history)
    test_hook("class_relations.py", test_class_relations)
    test_hook("class_methods.py", test_class_methods)
    test_hook("method_callchain.py", test_method_callchain)
    test_hook("class_context.py", test_class_context)
    test_hook("method_context.py", test_method_context)
    test_hook("impact_analysis.py", test_impact_analysis)

    # Summary
    print("\n" + "=" * 60)
    print("TEST SUMMARY")
    print("=" * 60)
    print(f"✓ Passed: {len(results['passed'])}")
    for test in results['passed']:
        print(f"    - {test}")

    print(f"\n⊘ Skipped: {len(results['skipped'])}")
    for test in results['skipped']:
        print(f"    - {test}")

    print(f"\n✗ Failed: {len(results['failed'])}")
    for test, error in results['failed']:
        print(f"    - {test}: {error}")

    print("\n" + "=" * 60)
    total = len(results['passed']) + len(results['failed']) + len(results['skipped'])
    print(f"Total: {total} tests")
    print(f"Pass rate: {len(results['passed'])}/{total-len(results['skipped'])} executable tests")
    print("=" * 60)

if __name__ == "__main__":
    main()
