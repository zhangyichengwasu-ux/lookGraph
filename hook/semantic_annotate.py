#!/usr/bin/env python3
"""
LookGraph 语义注释工具
简化的接口，用于快速创建业务注释
"""
import argparse
import sys
import subprocess
import os
from pathlib import Path

# 确保脚本目录在模块搜索路径中
sys.path.insert(0, str(Path(__file__).parent))

from lookgraph_client import get_client
from file_hash import hash_file_compressed

def get_git_hash(project_path):
    """自动获取当前 git commit hash"""
    try:
        result = subprocess.run(
            ['git', 'rev-parse', 'HEAD'],
            cwd=project_path,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except:
        return None

def get_file_hash_by_class(project_id, package, class_name):
    """通过 LookGraph 查询类的源文件路径，并计算文件 hash"""
    try:
        client = get_client()
        class_id = f"{package}.{class_name}"
        # 通过 class_context 获取 classNode 信息（含 filePath）
        data = client.get(f"/api/context/class/{class_id}")
        if not data:
            return None
        file_path = data.get("filePath") or (data.get("classNode") or {}).get("filePath")
        if not file_path or not os.path.exists(file_path):
            return None
        return hash_file_compressed(file_path)
    except Exception as e:
        print(f"  ⚠ 查询文件路径失败: {e}", file=sys.stderr)
        return None

def main():
    parser = argparse.ArgumentParser(
        description='创建或更新 LookGraph 语义注释',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 创建类注释
  %(prog)s --project-id proj123 --package com.example --class UserService \\
    --type CLASS --content "用户服务，负责注册登录" --source AI

  # 创建方法注释
  %(prog)s --project-id proj123 --package com.example --class UserService \\
    --method login --type METHOD --content "用户登录方法" --source AI

  # 人工修正
  %(prog)s --project-id proj123 --package com.example --class UserService \\
    --type CLASS --content "修正后的描述" --source HUMAN --reason "修正 AI 理解"
        """
    )

    # 必需参数
    parser.add_argument('--project-id', required=True, help='项目 ID')
    parser.add_argument('--package', required=True, help='包名，如 com.example')
    parser.add_argument('--class', dest='class_name', required=True, help='类名')
    parser.add_argument('--type', required=True,
                       choices=['CLASS', 'METHOD', 'FIELD', 'ENUM', 'INTERFACE', 'ABSTRACT_CLASS'],
                       help='注释类型')
    parser.add_argument('--content', required=True, help='业务语义描述')
    parser.add_argument('--source', required=True,
                       choices=['AI', 'HUMAN'],
                       help='注释来源（AI 生成或人工修正）')

    # 可选参数
    parser.add_argument('--method', help='方法名（注释方法时必填）')
    parser.add_argument('--field', help='字段名（注释字段时必填）')
    parser.add_argument('--reason', help='创建/修改原因')
    parser.add_argument('--node-id', help='Neo4j 节点 ID（如果已知）')
    parser.add_argument('--git-hash', help='Git commit hash（不提供则自动获取/通过文件 hash 计算）')
    parser.add_argument('--project-path', help='项目路径（用于自动获取 git hash）')

    args = parser.parse_args()

    # 获取代码 hash：优先级 --git-hash > git hash > 文件内容 hash
    git_hash = args.git_hash
    if not git_hash and args.project_path:
        git_hash = get_git_hash(args.project_path)
        if git_hash:
            print(f"自动获取 git hash: {git_hash[:8]}...")

    if not git_hash:
        # 回退到通过类源文件计算 hash
        print("git hash 不可用，尝试通过类源文件计算 hash...")
        git_hash = get_file_hash_by_class(args.project_id, args.package, args.class_name)
        if git_hash:
            print(f"使用文件内容 hash: {git_hash[:8]}...")

    if not git_hash:
        print("错误: 无法获取代码 hash", file=sys.stderr)
        print("  - 不是 git 仓库且未通过 LookGraph 找到源文件", file=sys.stderr)
        print("  - 请提供 --git-hash 或确保项目已在 LookGraph 中初始化", file=sys.stderr)
        sys.exit(1)

    # 构造请求体
    body = {
        "packageName": args.package,
        "className": args.class_name,
        "type": args.type,
        "content": args.content,
        "gitCommitHash": git_hash,
        "modifiedBy": args.source
    }

    if args.method:
        body["methodName"] = args.method
    if args.field:
        body["fieldName"] = args.field
    if args.reason:
        body["modifyReason"] = args.reason
    if args.node_id:
        body["neo4jNodeId"] = args.node_id

    # 调用 API
    client = get_client()
    try:
        print("创建语义注释...")
        result = client.post("/api/semantic", data=body)

        print("\n✓ 注释已保存")
        print(f"  类型: {args.type}")
        print(f"  实体: {args.package}.{args.class_name}", end='')
        if args.method:
            print(f"#{args.method}", end='')
        elif args.field:
            print(f".{args.field}", end='')
        print()
        print(f"  内容: {args.content}")
        print(f"  来源: {args.source}")
        if args.reason:
            print(f"  原因: {args.reason}")

        # 触发精确的向量更新（只更新这一条）
        history_id = result.get("historyId") if isinstance(result, dict) else None
        if history_id:
            print("\n正在更新向量索引...")
            client.post(f"/api/semantic/{history_id}/index", params={"projectId": args.project_id})
            print("✓ 向量索引已更新")
        else:
            print("\n⚠ 警告: 未获取到注释 ID，跳过向量更新")

        print("\n提示: 下次使用语义搜索时可以找到这个注释")

    except Exception as e:
        print(f"\n✗ 创建注释失败: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
