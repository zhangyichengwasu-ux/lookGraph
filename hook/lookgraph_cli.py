#!/usr/bin/env python3
"""LookGraph API 客户端脚本

用法:
  python lookgraph_client.py <command> [args...]

环境变量:
  LOOKGRAPH_URL - API 地址 (默认: http://localhost:8090)

Commands (项目管理):
  project-init --name NAME --path PATH [--language JAVA]
  project-update --project-id ID
  project-summary --project-id ID
  project-list

Commands (结构查询):
  class-relation --class-id CLASS_ID
  method-callchain --method-id METHOD_ID
  module-classes --module-id MODULE_ID
  class-methods --class-id CLASS_ID
  impact --entity-type TYPE --entity-id ID

Commands (语义检索):
  semantic-search --query QUERY [--module MODULE] [--top-k 10]

Commands (语义注释管理):
  semantic-create --package PKG --class CLASS [--method METHOD] --type TYPE --content CONTENT --git-hash HASH --modified-by SOURCE [--reason REASON]
  semantic-class-history --package PKG --class CLASS
  semantic-method-history --package PKG --class CLASS --method METHOD
  semantic-by-git --git-hash HASH
  semantic-by-node --node-id NODE_ID

Commands (上下文获取):
  context-method --method-id METHOD_ID
  context-class --class-id CLASS_ID

示例:
  # 初始化项目
  python lookgraph_client.py project-init --name my-project --path /path/to/project

  # 创建 AI 生成的方法注释
  python lookgraph_client.py semantic-create \\
    --package com.example --class UserService --method login \\
    --type METHOD --content "用户登录验证" \\
    --git-hash abc123 --modified-by AI --reason "自动生成"

  # 查询类的注释历史
  python lookgraph_client.py semantic-class-history --package com.example --class UserService

  # 语义检索
  python lookgraph_client.py semantic-search --query "支付重试逻辑" --top-k 5
"""
import argparse
import json
import sys
import os
import urllib.request
import urllib.error

BASE_URL = os.environ.get("LOOKGRAPH_URL", "http://localhost:8090")


def headers():
    return {"Content-Type": "application/json"}


def get(path):
    req = urllib.request.Request(f"{BASE_URL}{path}", headers=headers())
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def post(path, body=None):
    data = json.dumps(body or {}).encode()
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers(), method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)


def pp(data):
    print(json.dumps(data, ensure_ascii=False, indent=2))


# ── 项目管理 ──────────────────────────────────────────────────────────────────

def project_init(args):
    body = {
        "name": args.name,
        "path": args.path,
        "language": args.language or "JAVA"
    }
    pp(post("/api/project/init", body))


def project_update(args):
    pp(post(f"/api/project/update?projectId={args.project_id}"))


def project_summary(args):
    pp(get(f"/api/project/summary?projectId={args.project_id}"))


def project_list(args):
    pp(get("/api/project/list"))


# ── 结构查询 ──────────────────────────────────────────────────────────────────

def class_relation(args):
    pp(get(f"/api/structure/class/{args.class_id}/relation"))


def method_callchain(args):
    pp(get(f"/api/structure/method/{args.method_id}/callchain"))


def module_classes(args):
    pp(get(f"/api/structure/module/{args.module_id}/classes"))


def class_methods(args):
    pp(get(f"/api/structure/class/{args.class_id}/methods"))


def impact(args):
    pp(get(f"/api/structure/impact/{args.entity_type}/{args.entity_id}"))


# ── 语义检索 ──────────────────────────────────────────────────────────────────

def semantic_search(args):
    body = {"query": args.query, "topK": args.top_k}
    if args.module:
        body["module"] = args.module
    pp(post("/api/semantic/search", body))


# ── 语义注释管理 ──────────────────────────────────────────────────────────────

def semantic_create(args):
    body = {
        "packageName": args.package,
        "className": args.class_name,
        "type": args.type,
        "content": args.content,
        "gitCommitHash": args.git_hash,
        "modifiedBy": args.modified_by
    }
    if args.method:
        body["methodName"] = args.method
    if args.field:
        body["fieldName"] = args.field
    if args.node_id:
        body["neo4jNodeId"] = args.node_id
    if args.reason:
        body["modifyReason"] = args.reason
    pp(post("/api/semantic", body))


def semantic_class_history(args):
    pp(get(f"/api/semantic/class?packageName={args.package}&className={args.class_name}"))


def semantic_method_history(args):
    pp(get(f"/api/semantic/method?packageName={args.package}&className={args.class_name}&methodName={args.method}"))


def semantic_by_git(args):
    pp(get(f"/api/semantic/git/{args.git_hash}"))


def semantic_by_node(args):
    pp(get(f"/api/semantic/node/{args.node_id}"))


# ── 上下文获取 ────────────────────────────────────────────────────────────────

def context_method(args):
    pp(get(f"/api/context/method/{args.method_id}"))


def context_class(args):
    pp(get(f"/api/context/class/{args.class_id}"))


# ── CLI ───────────────────────────────────────────────────────────────────────

def main():
    p = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    sub = p.add_subparsers(dest="cmd", required=True)

    # 项目管理
    s = sub.add_parser("project-init")
    s.add_argument("--name", required=True)
    s.add_argument("--path", required=True)
    s.add_argument("--language")

    s = sub.add_parser("project-update")
    s.add_argument("--project-id", required=True)

    s = sub.add_parser("project-summary")
    s.add_argument("--project-id", required=True)

    sub.add_parser("project-list")

    # 结构查询
    s = sub.add_parser("class-relation")
    s.add_argument("--class-id", required=True)

    s = sub.add_parser("method-callchain")
    s.add_argument("--method-id", required=True)

    s = sub.add_parser("module-classes")
    s.add_argument("--module-id", required=True)

    s = sub.add_parser("class-methods")
    s.add_argument("--class-id", required=True)

    s = sub.add_parser("impact")
    s.add_argument("--entity-type", required=True, choices=["CLASS", "METHOD", "MODULE"])
    s.add_argument("--entity-id", required=True)

    # 语义检索
    s = sub.add_parser("semantic-search")
    s.add_argument("--query", required=True)
    s.add_argument("--module")
    s.add_argument("--top-k", type=int, default=10)

    # 语义注释管理
    s = sub.add_parser("semantic-create")
    s.add_argument("--package", required=True)
    s.add_argument("--class", dest="class_name", required=True)
    s.add_argument("--method")
    s.add_argument("--field")
    s.add_argument("--type", required=True, choices=["CLASS", "ENUM", "INTERFACE", "ABSTRACT_CLASS", "METHOD", "FIELD"])
    s.add_argument("--node-id")
    s.add_argument("--content")
    s.add_argument("--git-hash", required=True)
    s.add_argument("--modified-by", required=True, choices=["AI", "HUMAN"])
    s.add_argument("--reason")

    s = sub.add_parser("semantic-class-history")
    s.add_argument("--package", required=True)
    s.add_argument("--class", dest="class_name", required=True)

    s = sub.add_parser("semantic-method-history")
    s.add_argument("--package", required=True)
    s.add_argument("--class", dest="class_name", required=True)
    s.add_argument("--method", required=True)

    s = sub.add_parser("semantic-by-git")
    s.add_argument("--git-hash", required=True)

    s = sub.add_parser("semantic-by-node")
    s.add_argument("--node-id", required=True)

    # 上下文获取
    s = sub.add_parser("context-method")
    s.add_argument("--method-id", required=True)

    s = sub.add_parser("context-class")
    s.add_argument("--class-id", required=True)

    CMDS = {
        "project-init": project_init,
        "project-update": project_update,
        "project-summary": project_summary,
        "project-list": project_list,
        "class-relation": class_relation,
        "method-callchain": method_callchain,
        "module-classes": module_classes,
        "class-methods": class_methods,
        "impact": impact,
        "semantic-search": semantic_search,
        "semantic-create": semantic_create,
        "semantic-class-history": semantic_class_history,
        "semantic-method-history": semantic_method_history,
        "semantic-by-git": semantic_by_git,
        "semantic-by-node": semantic_by_node,
        "context-method": context_method,
        "context-class": context_class,
    }

    args = p.parse_args()
    CMDS[args.cmd](args)


if __name__ == "__main__":
    main()
