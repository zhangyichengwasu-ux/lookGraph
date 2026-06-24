#!/usr/bin/env python3
"""
LookGraph API Client Library
Shared client for all LookGraph hook scripts
"""

import requests
import json
import sys
import os
from typing import Optional, Dict, Any

class LookGraphClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or os.getenv("LOOKGRAPH_BASE_URL", "http://localhost:8090")
        self.session = requests.Session()
        self.session.trust_env = False  # Disable proxy detection for localhost
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })

    def _request(self, method: str, endpoint: str, **kwargs) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        try:
            response = self.session.request(method, url, **kwargs)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error calling {endpoint}: {e}", file=sys.stderr)
            if hasattr(e.response, 'text'):
                print(f"Response: {e.response.text}", file=sys.stderr)
            sys.exit(1)

    def get(self, endpoint: str, params: Optional[Dict] = None) -> Any:
        """GET request that returns parsed data"""
        response = self._request("GET", endpoint, params=params)
        # Return the data directly for programmatic use
        if response.get("code") == 0:
            return response.get("data")
        else:
            raise Exception(f"API Error: {response.get('message', 'Unknown error')}")

    def post(self, endpoint: str, data: Optional[Dict] = None, params: Optional[Dict] = None) -> Any:
        """POST request that returns parsed data"""
        response = self._request("POST", endpoint, json=data, params=params)
        # Return the data directly for programmatic use
        if response.get("code") == 0:
            return response.get("data")
        else:
            raise Exception(f"API Error: {response.get('message', 'Unknown error')}")

    def format_output(self, data: Any) -> str:
        """Format data as JSON string for display"""
        return json.dumps(data, indent=2, ensure_ascii=False)

    def get_data(self, response: Dict[str, Any]) -> Any:
        """Extract data from API response"""
        if response.get("code") == 0:
            return response.get("data")
        else:
            return response

def get_client() -> LookGraphClient:
    return LookGraphClient()
