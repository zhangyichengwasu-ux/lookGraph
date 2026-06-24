#!/usr/bin/env python3
import sys
import hashlib
import re
from pathlib import Path


def hash_file_compressed(file_path: str) -> str:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    compressed = re.sub(r'\s+', '', content)
    hasher = hashlib.sha256()
    hasher.update(compressed.encode('utf-8'))
    return hasher.hexdigest()


def main():
    if len(sys.argv) != 2:
        print("用法: python file_hash.py <file_path>", file=sys.stderr)
        sys.exit(1)

    file_path = sys.argv[1]

    if not Path(file_path).exists():
        print(f"错误: 文件不存在 - {file_path}", file=sys.stderr)
        sys.exit(1)

    try:
        print(hash_file_compressed(file_path))
    except UnicodeDecodeError:
        print(f"错误: 文件编码不是 UTF-8 - {file_path}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
