#!/bin/bash
# LookGraph 数据清理脚本
# 清理 Neo4j、ChromaDB、MySQL 中的所有测试数据

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-Zyc163000!@#}"

CHROMA_URL="${CHROMA_URL:-http://localhost:8000}"

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-lookgraph}"

# 结果统计
SUCCESS_COUNT=0
TOTAL_COUNT=0

# 帮助信息
show_help() {
    cat << EOF
LookGraph 数据清理脚本

用法: $0 [选项]

选项:
  --neo4j         仅清理 Neo4j
  --chroma        仅清理 ChromaDB
  --mysql         仅清理 MySQL
  --all           清理所有数据库（默认）
  -y, --yes       跳过确认提示
  -h, --help      显示帮助信息

环境变量:
  NEO4J_URI       Neo4j 连接地址 (默认: bolt://localhost:7687)
  NEO4J_USER      Neo4j 用户名 (默认: neo4j)
  NEO4J_PASSWORD  Neo4j 密码 (默认: Zyc163000!@#)
  CHROMA_URL      ChromaDB 地址 (默认: http://localhost:8000)
  MYSQL_HOST      MySQL 主机 (默认: localhost)
  MYSQL_PORT      MySQL 端口 (默认: 3306)
  MYSQL_USER      MySQL 用户 (默认: root)
  MYSQL_PASSWORD  MySQL 密码 (默认: 空)
  MYSQL_DATABASE  MySQL 数据库 (默认: lookgraph)

示例:
  $0                  # 清理所有数据库（需要确认）
  $0 -y               # 清理所有数据库（跳过确认）
  $0 --mysql          # 仅清理 MySQL
  $0 --neo4j --chroma # 清理 Neo4j 和 ChromaDB
EOF
}

# Neo4j 清理
clean_neo4j() {
    echo ""
    echo -e "${YELLOW}[Neo4j] 开始清理...${NC}"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))

    # 检查 cypher-shell 是否可用
    if ! command -v cypher-shell &> /dev/null; then
        echo -e "  ${YELLOW}⚠️  cypher-shell 未安装，跳过 Neo4j 清理${NC}"
        echo "  提示: 安装 Neo4j Desktop 或命令行工具"
        return 1
    fi

    # 统计节点数量
    NODE_COUNT=$(cypher-shell -a "$NEO4J_URI" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" \
        "MATCH (n) RETURN count(n) as count" --format plain 2>/dev/null | tail -1 | awk '{print $1}')

    # 统计关系数量
    REL_COUNT=$(cypher-shell -a "$NEO4J_URI" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" \
        "MATCH ()-[r]->() RETURN count(r) as count" --format plain 2>/dev/null | tail -1 | awk '{print $1}')

    echo "  发现 ${NODE_COUNT} 个节点, ${REL_COUNT} 个关系"

    if [ "$NODE_COUNT" -gt 0 ] || [ "$REL_COUNT" -gt 0 ]; then
        # 删除所有节点和关系
        cypher-shell -a "$NEO4J_URI" -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" \
            "MATCH (n) DETACH DELETE n" &>/dev/null
        echo -e "  ${GREEN}✓ 已删除所有节点和关系${NC}"
    else
        echo -e "  ${GREEN}✓ 数据库已经是空的${NC}"
    fi

    echo -e "${GREEN}[Neo4j] 清理完成 ✓${NC}"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    return 0
}

# ChromaDB 清理
clean_chroma() {
    echo ""
    echo -e "${YELLOW}[ChromaDB] 开始清理...${NC}"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))

    # 获取所有集合
    COLLECTIONS=$(curl -s -X GET \
        "${CHROMA_URL}/api/v2/tenants/default_tenant/databases/default_database/collections" \
        -H "Content-Type: application/json" 2>/dev/null)

    if [ $? -ne 0 ]; then
        echo -e "  ${YELLOW}⚠️  无法连接到 ChromaDB${NC}"
        echo "  提示: 确保 ChromaDB 运行在 ${CHROMA_URL}"
        return 1
    fi

    # 解析集合数量
    COLL_COUNT=$(echo "$COLLECTIONS" | grep -o '"id"' | wc -l | tr -d ' ')

    if [ "$COLL_COUNT" -eq 0 ]; then
        echo -e "  ${GREEN}✓ 没有集合需要删除${NC}"
        echo -e "${GREEN}[ChromaDB] 清理完成 ✓${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        return 0
    fi

    echo "  发现 ${COLL_COUNT} 个集合"

    # 删除每个集合
    echo "$COLLECTIONS" | grep -o '"id":"[^"]*"' | cut -d'"' -f4 | while read -r COLL_ID; do
        COLL_NAME=$(echo "$COLLECTIONS" | grep -A 1 "\"id\":\"$COLL_ID\"" | grep '"name"' | cut -d'"' -f4)

        curl -s -X DELETE \
            "${CHROMA_URL}/api/v2/tenants/default_tenant/databases/default_database/collections/${COLL_ID}" \
            -H "Content-Type: application/json" &>/dev/null

        echo -e "  ${GREEN}✓ 已删除集合: ${COLL_NAME}${NC}"
    done

    echo -e "${GREEN}[ChromaDB] 清理完成 ✓${NC}"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    return 0
}

# MySQL 清理
clean_mysql() {
    echo ""
    echo -e "${YELLOW}[MySQL] 开始清理...${NC}"
    TOTAL_COUNT=$((TOTAL_COUNT + 1))

    # 检查 mysql 客户端是否可用
    if ! command -v mysql &> /dev/null; then
        echo -e "  ${YELLOW}⚠️  mysql 客户端未安装，跳过 MySQL 清理${NC}"
        echo "  提示: 安装 MySQL 客户端工具"
        return 1
    fi

    # 构建 mysql 命令
    MYSQL_CMD="mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER}"
    if [ -n "$MYSQL_PASSWORD" ]; then
        MYSQL_CMD="$MYSQL_CMD -p${MYSQL_PASSWORD}"
    fi

    # 查询记录数
    COUNT=$($MYSQL_CMD -N -e "USE ${MYSQL_DATABASE}; SELECT COUNT(*) FROM semantic_history;" 2>/dev/null)

    if [ $? -ne 0 ]; then
        echo -e "  ${YELLOW}⚠️  无法连接到 MySQL${NC}"
        return 1
    fi

    echo "  发现 ${COUNT} 条记录"

    if [ "$COUNT" -gt 0 ]; then
        # 清空表
        $MYSQL_CMD -e "USE ${MYSQL_DATABASE}; DELETE FROM semantic_history;" 2>/dev/null
        echo -e "  ${GREEN}✓ 已删除 ${COUNT} 条记录${NC}"
    else
        echo -e "  ${GREEN}✓ 表已经是空的${NC}"
    fi

    echo -e "${GREEN}[MySQL] 清理完成 ✓${NC}"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    return 0
}

# 主函数
main() {
    # 解析参数
    CLEAN_NEO4J=false
    CLEAN_CHROMA=false
    CLEAN_MYSQL=false
    SKIP_CONFIRM=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --neo4j)
                CLEAN_NEO4J=true
                shift
                ;;
            --chroma)
                CLEAN_CHROMA=true
                shift
                ;;
            --mysql)
                CLEAN_MYSQL=true
                shift
                ;;
            --all)
                CLEAN_NEO4J=true
                CLEAN_CHROMA=true
                CLEAN_MYSQL=true
                shift
                ;;
            -y|--yes)
                SKIP_CONFIRM=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 如果没有指定任何选项，默认清理所有
    if [ "$CLEAN_NEO4J" = false ] && [ "$CLEAN_CHROMA" = false ] && [ "$CLEAN_MYSQL" = false ]; then
        CLEAN_NEO4J=true
        CLEAN_CHROMA=true
        CLEAN_MYSQL=true
    fi

    echo "============================================================"
    echo "LookGraph 数据清理脚本"
    echo "============================================================"

    echo ""
    echo "将清理以下数据库:"
    [ "$CLEAN_NEO4J" = true ] && echo "  • Neo4j (图谱数据)"
    [ "$CLEAN_CHROMA" = true ] && echo "  • ChromaDB (向量数据)"
    [ "$CLEAN_MYSQL" = true ] && echo "  • MySQL (语义注释)"

    # 确认提示
    if [ "$SKIP_CONFIRM" = false ]; then
        echo ""
        echo -ne "${YELLOW}⚠️  此操作将删除所有数据，是否继续? [y/N]: ${NC}"
        read -r CONFIRM
        if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
            echo ""
            echo "已取消清理操作"
            exit 0
        fi
    fi

    echo ""
    echo "开始清理数据..."

    # 执行清理
    [ "$CLEAN_NEO4J" = true ] && clean_neo4j || true
    [ "$CLEAN_CHROMA" = true ] && clean_chroma || true
    [ "$CLEAN_MYSQL" = true ] && clean_mysql || true

    # 输出总结
    echo ""
    echo "============================================================"
    echo "清理结果总结"
    echo "============================================================"

    if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
        echo -e "${GREEN}✓ 所有数据已清理完成 (${SUCCESS_COUNT}/${TOTAL_COUNT})${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  部分数据库清理失败 (${SUCCESS_COUNT}/${TOTAL_COUNT})${NC}"
        exit 1
    fi
}

main "$@"
