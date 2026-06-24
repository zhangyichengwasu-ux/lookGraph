#!/usr/bin/env python3
"""
LookGraph 数据清理脚本
清理 Neo4j、ChromaDB、MySQL 中的所有测试数据

用法:
  python clean.py [--neo4j] [--chroma] [--mysql] [--all]

  不带参数时，默认清理所有数据

环境变量:
  NEO4J_URI       - Neo4j 连接地址 (默认: bolt://localhost:7687)
  NEO4J_USER      - Neo4j 用户名 (默认: neo4j)
  NEO4J_PASSWORD  - Neo4j 密码 (默认: Zyc163000!@#)
  CHROMA_URL      - ChromaDB 地址 (默认: http://localhost:8000)
  MYSQL_HOST      - MySQL 主机 (默认: localhost)
  MYSQL_PORT      - MySQL 端口 (默认: 3306)
  MYSQL_USER      - MySQL 用户 (默认: root)
  MYSQL_PASSWORD  - MySQL 密码 (默认: 空)
  MYSQL_DATABASE  - MySQL 数据库 (默认: lookgraph)
"""

import argparse
import os
import sys
import urllib.request
import urllib.error
import json


# ============================================================================
# 配置
# ============================================================================

NEO4J_URI = os.environ.get("NEO4J_URI", "bolt://localhost:7687")
NEO4J_USER = os.environ.get("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.environ.get("NEO4J_PASSWORD", "Zyc163000!@#")

CHROMA_URL = os.environ.get("CHROMA_URL", "http://localhost:8000")

MYSQL_HOST = os.environ.get("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "")
MYSQL_DATABASE = os.environ.get("MYSQL_DATABASE", "lookgraph")


# ============================================================================
# Neo4j 清理
# ============================================================================

def clean_neo4j():
    """清理 Neo4j 数据库"""
    print("\n[Neo4j] 开始清理...")

    try:
        from neo4j import GraphDatabase
    except ImportError:
        print("  ⚠️  neo4j 包未安装，跳过 Neo4j 清理")
        print("  提示: pip install neo4j")
        return False

    try:
        driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))

        with driver.session() as session:
            # 统计节点和关系数量
            result = session.run("MATCH (n) RETURN count(n) as node_count")
            node_count = result.single()["node_count"]

            result = session.run("MATCH ()-[r]->() RETURN count(r) as rel_count")
            rel_count = result.single()["rel_count"]

            print(f"  发现 {node_count} 个节点, {rel_count} 个关系")

            if node_count > 0 or rel_count > 0:
                # 删除所有节点和关系
                session.run("MATCH (n) DETACH DELETE n")
                print(f"  ✓ 已删除所有节点和关系")
            else:
                print(f"  ✓ 数据库已经是空的")

        driver.close()
        print("[Neo4j] 清理完成 ✓")
        return True

    except Exception as e:
        print(f"  ✗ 清理失败: {e}")
        return False


# ============================================================================
# ChromaDB 清理
# ============================================================================

def clean_chroma():
    """清理 ChromaDB 数据"""
    print("\n[ChromaDB] 开始清理...")

    try:
        # 获取所有集合
        req = urllib.request.Request(
            f"{CHROMA_URL}/api/v2/tenants/default_tenant/databases/default_database/collections",
            headers={"Content-Type": "application/json"}
        )

        with urllib.request.urlopen(req, timeout=10) as response:
            collections = json.loads(response.read())

        if not collections:
            print("  ✓ 没有集合需要删除")
            print("[ChromaDB] 清理完成 ✓")
            return True

        print(f"  发现 {len(collections)} 个集合")

        # 删除每个集合
        for collection in collections:
            coll_name = collection.get("name")
            coll_id = collection.get("id")

            try:
                delete_req = urllib.request.Request(
                    f"{CHROMA_URL}/api/v2/tenants/default_tenant/databases/default_database/collections/{coll_id}",
                    method="DELETE",
                    headers={"Content-Type": "application/json"}
                )

                with urllib.request.urlopen(delete_req, timeout=10) as response:
                    print(f"  ✓ 已删除集合: {coll_name}")

            except Exception as e:
                print(f"  ✗ 删除集合 {coll_name} 失败: {e}")

        print("[ChromaDB] 清理完成 ✓")
        return True

    except urllib.error.URLError as e:
        print(f"  ⚠️  无法连接到 ChromaDB: {e}")
        print(f"  提示: 确保 ChromaDB 运行在 {CHROMA_URL}")
        return False
    except Exception as e:
        print(f"  ✗ 清理失败: {e}")
        return False


# ============================================================================
# MySQL 清理
# ============================================================================

def clean_mysql():
    """清理 MySQL 数据库"""
    print("\n[MySQL] 开始清理...")

    try:
        import mysql.connector
    except ImportError:
        print("  ⚠️  mysql-connector-python 包未安装，跳过 MySQL 清理")
        print("  提示: pip install mysql-connector-python")
        return False

    try:
        conn = mysql.connector.connect(
            host=MYSQL_HOST,
            port=MYSQL_PORT,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            database=MYSQL_DATABASE
        )

        cursor = conn.cursor()

        # 查询表数据量
        cursor.execute("SELECT COUNT(*) FROM semantic_history")
        count = cursor.fetchone()[0]

        print(f"  发现 {count} 条记录")

        if count > 0:
            # 清空表
            cursor.execute("DELETE FROM semantic_history")
            conn.commit()
            print(f"  ✓ 已删除 {count} 条记录")
        else:
            print(f"  ✓ 表已经是空的")

        cursor.close()
        conn.close()

        print("[MySQL] 清理完成 ✓")
        return True

    except Exception as e:
        print(f"  ✗ 清理失败: {e}")
        return False


# ============================================================================
# 主函数
# ============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="LookGraph 数据清理脚本",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument("--neo4j", action="store_true", help="仅清理 Neo4j")
    parser.add_argument("--chroma", action="store_true", help="仅清理 ChromaDB")
    parser.add_argument("--mysql", action="store_true", help="仅清理 MySQL")
    parser.add_argument("--all", action="store_true", help="清理所有数据库（默认）")
    parser.add_argument("-y", "--yes", action="store_true", help="跳过确认提示")

    args = parser.parse_args()

    # 如果没有指定任何选项，默认清理所有
    clean_all = args.all or not (args.neo4j or args.chroma or args.mysql)

    print("=" * 60)
    print("LookGraph 数据清理脚本")
    print("=" * 60)

    if clean_all:
        print("\n将清理以下数据库:")
        print("  • Neo4j (图谱数据)")
        print("  • ChromaDB (向量数据)")
        print("  • MySQL (语义注释)")
    else:
        print("\n将清理以下数据库:")
        if args.neo4j:
            print("  • Neo4j (图谱数据)")
        if args.chroma:
            print("  • ChromaDB (向量数据)")
        if args.mysql:
            print("  • MySQL (语义注释)")

    # 确认提示
    if not args.yes:
        confirm = input("\n⚠️  此操作将删除所有数据，是否继续? [y/N]: ")
        if confirm.lower() != 'y':
            print("\n已取消清理操作")
            return 0

    print("\n开始清理数据...")

    results = []

    # 执行清理
    if clean_all or args.neo4j:
        results.append(("Neo4j", clean_neo4j()))

    if clean_all or args.chroma:
        results.append(("ChromaDB", clean_chroma()))

    if clean_all or args.mysql:
        results.append(("MySQL", clean_mysql()))

    # 输出总结
    print("\n" + "=" * 60)
    print("清理结果总结")
    print("=" * 60)

    success_count = sum(1 for _, success in results if success)
    total_count = len(results)

    for name, success in results:
        status = "✓" if success else "✗"
        print(f"  {status} {name}")

    print(f"\n完成: {success_count}/{total_count} 个数据库清理成功")

    if success_count == total_count:
        print("\n✓ 所有数据已清理完成")
        return 0
    else:
        print("\n⚠️  部分数据库清理失败，请检查日志")
        return 1


if __name__ == "__main__":
    sys.exit(main())
