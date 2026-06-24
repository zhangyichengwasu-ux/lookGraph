---
name: look_graph/init
description: 遍历 Java 项目所有方法与构造器，生成注解并写入包目录 MD 文件，同步更新到 Neo4j。
---

# /look_graph/init

## 触发方式

用户输入 `/look_graph/init [path]`，其中 `path` 为目标 Java 项目根目录（绝对路径）。若未提供，使用当前工作目录。

---

## 执行流程

### Step 1 — 发现所有 Java 文件

```bash
find {path} -name "*.java" -not -path "*/test/*"
```

对每个 `.java` 文件执行 Step 2 ~ Step 4。

---

### Step 2 — 提取方法与构造器

读取文件内容，识别：
- **类名**、**包名**（`package` 声明）
- 所有 **public/protected 方法** 的签名：方法名、参数列表（名称+类型）、返回类型、声明的受检异常
- 所有 **构造器** 的签名：参数列表、声明的受检异常
- **跳过**：getter/setter（方法名以 `get`/`set`/`is` 开头且方法体只有单行 return/赋值）
- 已有 Javadoc（`/** ... */`）的跳过，不重复生成

`methodId` 格式：`{全限定类名}#{方法名}({参数类型,...})`，例如：
```
com.example.service.OrderService#createOrder(OrderRequest)
```

---

### Step 3 — 自上而下解读业务逻辑并生成注解

**遍历顺序**：Controller → Service → ServiceImpl → Mapper/Repository，沿调用链自上而下，每解读一个方法时已知其上层调用者的语义。

对每个需生成注解的方法：

1. **通读方法体全部代码**，理解实际执行逻辑，而非仅看签名。
2. **若所在类是 Mapper/DAO**（接口名含 `Mapper`/`Dao`，或有 `@Mapper` 注解）：
   - 找到对应的 MyBatis XML 文件（与接口同名，位于 `resources/` 下同包路径）
   - 读取 XML 中对应 `<select>`/`<insert>`/`<update>`/`<delete>` 标签，理解实际 SQL 语义
   - 注解描述必须反映 SQL 实际做了什么（查询条件、关联表、返回内容等）
3. **若当前包目录已有 `ANNOTATIONS.md`**，先读取其中已有条目，作为该类职责的背景上下文，辅助理解当前方法。

生成格式：
```
/**
 * {一句话描述，基于实际业务逻辑，以动词开头}
 *
 * @param {paramName} {参数含义}
 * @return {返回值含义}（void 方法省略）
 * @throws {ExceptionType} {触发条件}（无声明异常省略）
 */
```

---

### Step 4 — 写入包目录 MD 文件

每个 Java 包对应一个 MD 文件，路径为：

```
{java_source_root}/{package/path}/ANNOTATIONS.md
```

例如，包 `com.example.service` 对应：

```
src/main/java/com/example/service/ANNOTATIONS.md
```

**写入规则**：
- 若文件不存在，新建并写入
- 若文件已存在（说明该包曾被解读过）：先读取全文作为背景上下文，再对比当前解读结果——若新理解与已有描述有出入，**修正**该条目；若有新发现（如该方法还涉及其他逻辑），**补充**到描述中；无变化则保持原样
- 每次处理完一个包立即写入，不批量缓存

文件格式：

```markdown
# Annotations: com.example.service

## OrderService

### createOrder(OrderRequest)
```java
/**
 * 创建订单并返回初始化后的订单实体。
 *
 * @param request 订单创建请求参数
 * @return 已持久化的订单对象
 */
```

### OrderService(OrderRepository, PaymentService)
```java
/**
 * 注入订单仓储与支付服务，完成依赖初始化。
 *
 * @param orderRepository 订单数据访问对象
 * @param paymentService  支付服务
 */
```
```

---

### Step 5 — 同步到 Neo4j

**方式 A（优先）**：通过 lookGraph API 更新，服务地址 `http://localhost:8090`：

```bash
# 若已有 /api/method/{methodId}/comment 端点
curl -X PATCH "http://localhost:8090/api/method/{methodId}/comment" \
  -H "Content-Type: application/json" \
  -d '{"comment": "{生成的注解文本}"}'
```

**方式 B**：若 API 不可用，直接通过 `cypher-shell` 写入 Neo4j：

```cypher
MATCH (m:Method {methodId: $methodId})
SET m.comment = $comment
```

```bash
cypher-shell -u neo4j -p password \
  "MATCH (m:Method {methodId: '$methodId'}) SET m.comment = '$comment'"
```

**方式 C**：若 cypher-shell 不可用，将生成的 Javadoc 写回源文件对应方法上方，然后调用：

```bash
curl -X POST "http://localhost:8090/api/project/update?projectId={projectId}"
```

触发增量扫描，让 lookGraph 重新解析并写入 Neo4j。

---

## 并发策略

- 按包并发处理，同一包内文件顺序处理，避免 MD 文件写冲突
- 每处理完一个文件，立即写 MD 并更新 Neo4j，不缓存到最后统一写入

---

## 输出汇总

完成后向用户报告：

```
已处理 {N} 个 Java 文件
生成注解 {M} 条（跳过已有 Javadoc {K} 条）
写入 MD 文件 {P} 个
Neo4j 更新 {M} 条
```
