#!/usr/bin/env python3
"""
LookGraph 语义注释工具
简化的接口，用于快速创建业务注释
"""
import argparse
import sys
import subprocess
from lookgraph_client import get_client

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
    parser.add_argument('--git-hash', help='Git commit hash（不提供则自动获取）')
    parser.add_argument('--project-path', help='项目路径（用于自动获取 git hash）')

    args = parser.parse_args()

    # 获取 git hash
    git_hash = args.git_hash
    if not git_hash and args.project_path:
        git_hash = get_git_hash(args.project_path)
        if git_hash:
            print(f"自动获取 git hash: {git_hash[:8]}...")

    if not git_hash:
        print("错误: 需要提供 --git-hash 或 --project-path", file=sys.stderr)
        print("提示: 使用 'git rev-parse HEAD' 获取当前 commit hash", file=sys.stderr)
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

        # 触发向量更新
        print("\n正在更新向量索引...")
        client.post(f"/api/project/update", params={"projectId": args.project_id})
        print("✓ 向量索引已更新")

        print("\n提示: 下次使用语义搜索时可以找到这个注释")

    except Exception as e:
        print(f"\n✗ 创建注释失败: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
