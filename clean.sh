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

    # 从 bolt URI 转换为 HTTP URL
    NEO4J_HTTP_URL="${NEO4J_URI/bolt:\/\//http://}"
    NEO4J_HTTP_URL="${NEO4J_HTTP_URL/:7687/:7474}"

    # 使用 HTTP API 执行 Cypher 查询
    neo4j_query() {
        local query="$1"
        curl -s -u "${NEO4J_USER}:${NEO4J_PASSWORD}" \
            -X POST "${NEO4J_HTTP_URL}/db/neo4j/tx/commit" \
            -H "Content-Type: application/json" \
            -d "{\"statements\":[{\"statement\":\"${query}\"}]}"
    }

    # 测试连接
    TEST_RESULT=$(neo4j_query "RETURN 1")
    if echo "$TEST_RESULT" | grep -q '"errors":\[\]'; then
        :
    else
        echo -e "  ${RED}✗ 无法连接到 Neo4j${NC}"
        echo "  请检查连接配置: ${NEO4J_HTTP_URL}"
        return 1
    fi

    # 统计节点数量
    NODE_RESULT=$(neo4j_query "MATCH (n) RETURN count(n) as count")
    NODE_COUNT=$(echo "$NODE_RESULT" | grep -o '"row":\[[0-9]*\]' | grep -o '[0-9]*' | head -1)
    NODE_COUNT=${NODE_COUNT:-0}

    # 统计关系数量
    REL_RESULT=$(neo4j_query "MATCH ()-[r]->() RETURN count(r) as count")
    REL_COUNT=$(echo "$REL_RESULT" | grep -o '"row":\[[0-9]*\]' | grep -o '[0-9]*' | head -1)
    REL_COUNT=${REL_COUNT:-0}

    echo "  发现 ${NODE_COUNT} 个节点, ${REL_COUNT} 个关系"

    if [ "$NODE_COUNT" -gt 0 ] 2>/dev/null || [ "$REL_COUNT" -gt 0 ] 2>/dev/null; then
        # 批量删除所有节点和关系，避免超时
        echo "  开始批量删除..."
        BATCH_SIZE=1000
        ITERATION=0

        while true; do
            # 检查剩余节点数
            REMAINING_RESULT=$(neo4j_query "MATCH (n) RETURN count(n) as count")
            REMAINING=$(echo "$REMAINING_RESULT" | grep -o '"row":\[[0-9]*\]' | grep -o '[0-9]*' | head -1)
            REMAINING=${REMAINING:-0}

            if [ "$REMAINING" -eq 0 ] 2>/dev/null; then
                break
            fi

            # 批量删除节点
            neo4j_query "MATCH (n) WITH n LIMIT ${BATCH_SIZE} DETACH DELETE n" >/dev/null

            ITERATION=$((ITERATION + 1))
            echo -e "  ${YELLOW}第 ${ITERATION} 批: 剩余 ~${REMAINING} 个节点...${NC}"

            # 避免无限循环
            if [ $ITERATION -gt 1000 ]; then
                echo -e "  ${RED}⚠️  删除迭代次数过多，可能存在问题${NC}"
                break
            fi
        done

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
