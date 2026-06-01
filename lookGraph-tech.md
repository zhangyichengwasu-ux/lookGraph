# Claude 代码地图助手 - 技术设计文档

## 文档信息
- 文档名称：Claude 代码地图助手技术设计
- 对应产品文档：lookGraph.md V1.0
- 技术栈：Spring Boot 4.0.6 + JDK 21 + Neo4j 5.x + ChromaDB
- 文档版本：V1.0
- 编写日期：2026-05-28

---

## 1. 技术栈总览

### 1.1 核心技术选型

| 层级 | 技术 | 版本 | 用途 |
| ---- | ---- | ---- | ---- |
| 运行时 | JDK | 21 (LTS) | 虚拟线程、Pattern Matching、Records |
| 框架 | Spring Boot | 4.0.6 | 应用容器、自动配置、依赖注入 |
| 框架 | Spring Framework | 7.x | 核心容器（Spring Boot 4 内置） |
| Web | Spring Web MVC | 7.x | RESTful API |
| 图数据库 | Neo4j | 5.x 社区版 | 代码结构图谱 |
| 图数据库驱动 | Spring Data Neo4j | 8.x | OGM、声明式仓储 |
| 向量数据库 | ChromaDB | 0.5.x+ | 语义向量存储 |
| 向量库客户端 | chromadb-java-client | 0.2.x | ChromaDB HTTP 封装 |
| Java AST 解析 | JavaParser | 3.26.x | Java 源码语法树解析 |
| Python AST 解析 | Jython / 外部进程 | - | 通过子进程调用 Python ast 模块 |
| 文件监听 | java.nio.file.WatchService | JDK 内置 | 增量扫描触发 |
| Embedding | Spring AI | 1.0.x | 文本向量化（OpenAI / 本地模型） |
| 配置 | Spring Boot Configuration | - | application.yml 集中管理 |
| 文档 | springdoc-openapi | 2.6.x | OpenAPI 3 / Swagger UI |
| 日志 | SLF4J + Logback | - | 结构化日志 |
| 构建 | Maven | 3.9+ | 依赖管理、打包 |
| 测试 | JUnit 5 + Testcontainers | - | 单元 / 集成测试 |

### 1.2 关键依赖（Maven）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
</parent>

<properties>
    <java.version>21</java.version>
    <javaparser.version>3.26.2</javaparser.version>
    <chromadb-java.version>0.2.0</chromadb-java.version>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-neo4j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- AST 解析 -->
    <dependency>
        <groupId>com.github.javaparser</groupId>
        <artifactId>javaparser-symbol-solver-core</artifactId>
        <version>${javaparser.version}</version>
    </dependency>

    <!-- ChromaDB Java 客户端 -->
    <dependency>
        <groupId>io.github.amikos-tech</groupId>
        <artifactId>chromadb-java-client</artifactId>
        <version>${chromadb-java.version}</version>
    </dependency>

    <!-- Spring AI Embedding -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>${spring-ai.version}</version>
    </dependency>

    <!-- API 文档 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>neo4j</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 2. 应用分层架构

```
┌──────────────────────────────────────────────────────────────┐
│  Controller 层 (REST API)                                    │
│  ProjectController | StructureController                      │
│  SemanticController | ContextController                       │
└─────────────────────────┬────────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────────┐
│  Service 层 (业务编排)                                       │
│  ProjectService | StructureQueryService                       │
│  SemanticSearchService | ContextAssemblyService               │
│  ImpactAnalysisService | SummaryService                       │
└─────────────────────────┬────────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────────┐
│  Domain 层 (领域模型 + 仓储接口)                            │
│  Node 实体: Project / Module / Class / Method                 │
│  Repository: Spring Data Neo4j 接口                           │
└─────────────────────────┬────────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────────┐
│  Infrastructure 层 (基础设施)                                │
│  Neo4j Driver | ChromaDB Client | Embedding Provider          │
│  AST Parser (JavaParser / PyAst) | FileWatcher                │
└──────────────────────────────────────────────────────────────┘
```

### 2.1 Maven 模块包结构

```
com.lookgraph
├── LookGraphApplication.java            # @SpringBootApplication 启动类
├── config
│   ├── Neo4jConfig.java                 # Neo4j 驱动 / 事务配置
│   ├── ChromaConfig.java                # ChromaDB 客户端配置
│   ├── EmbeddingConfig.java             # Embedding Provider 配置
│   ├── AsyncConfig.java                 # 虚拟线程 Executor
│   └── OpenApiConfig.java               # Swagger 配置
├── controller
│   ├── ProjectController.java
│   ├── StructureController.java
│   ├── SemanticController.java
│   └── ContextController.java
├── service
│   ├── ProjectService.java
│   ├── StructureQueryService.java
│   ├── SemanticSearchService.java
│   ├── ContextAssemblyService.java
│   ├── ImpactAnalysisService.java
│   └── SummaryService.java
├── domain
│   ├── node                             # Neo4j @Node 实体
│   │   ├── ProjectNode.java
│   │   ├── ModuleNode.java
│   │   ├── ClassNode.java
│   │   └── MethodNode.java
│   ├── relation                         # @RelationshipProperties
│   │   ├── BelongsToRelation.java
│   │   ├── ExtendsRelation.java
│   │   ├── ImplementsRelation.java
│   │   ├── CallsRelation.java
│   │   └── DependsOnRelation.java
│   └── repository                       # Spring Data Neo4j Repository
│       ├── ProjectRepository.java
│       ├── ModuleRepository.java
│       ├── ClassRepository.java
│       └── MethodRepository.java
├── parser                               # 代码解析层
│   ├── CodeParser.java                  # 解析器统一接口
│   ├── ParseResult.java                 # 解析结果 record
│   ├── java
│   │   ├── JavaAstParser.java           # JavaParser 封装
│   │   ├── JavaEntityExtractor.java     # 实体抽取
│   │   └── JavaRelationExtractor.java   # 关系抽取
│   └── python
│       └── PythonAstParser.java         # Python 子进程解析
├── vector                               # 向量库适配
│   ├── ChromaClient.java
│   ├── EmbeddingProvider.java
│   └── VectorIndexService.java
├── scanner
│   ├── ProjectScanner.java              # 全量扫描
│   ├── IncrementalScanner.java          # 增量扫描
│   └── FileWatcher.java                 # WatchService 监听
├── dto                                  # 入参 / 出参 DTO (record)
│   ├── request/
│   └── response/
└── common
    ├── exception/                        # 业务异常 + 全局 Handler
    ├── enums/
    └── util/
```

---

## 3. 数据模型实现

### 3.1 Neo4j 节点实体（Spring Data Neo4j）

#### ProjectNode

```java
@Node("Project")
public class ProjectNode {

    @Id
    private String projectId;

    private String name;
    private String path;
    private String techStack;
    private Instant createTime;
    private Instant updateTime;

    @Relationship(type = "BELONGS_TO", direction = Direction.INCOMING)
    private List<ModuleNode> modules = new ArrayList<>();

    // 构造方法、getter / setter 略
}
```

#### ModuleNode

```java
@Node("Module")
public class ModuleNode {

    @Id
    private String moduleId;

    private String name;
    private String businessTag;
    private String projectId;

    @Relationship(type = "DEPENDS_ON", direction = Direction.OUTGOING)
    private Set<ModuleNode> dependsOn = new HashSet<>();
}
```

#### ClassNode

```java
@Node("Class")
public class ClassNode {

    @Id
    private String classId;

    private String name;
    private String filePath;
    private String comment;
    private String moduleId;

    /** CLASS / INTERFACE / ENUM / RECORD */
    private ClassType type;

    @Relationship(type = "EXTENDS", direction = Direction.OUTGOING)
    private ClassNode parent;

    @Relationship(type = "IMPLEMENTS", direction = Direction.OUTGOING)
    private Set<ClassNode> interfaces = new HashSet<>();

    @Relationship(type = "DEPENDS_ON", direction = Direction.OUTGOING)
    private Set<ClassNode> dependsOn = new HashSet<>();
}
```

#### MethodNode

```java
@Node("Method")
public class MethodNode {

    @Id
    private String methodId;

    private String name;
    private String params;        // JSON 序列化参数列表
    private String returnType;
    private String comment;
    private String classId;
    private int startLine;
    private int endLine;

    @Relationship(type = "CALLS", direction = Direction.OUTGOING)
    private Set<MethodNode> calls = new HashSet<>();
}
```

### 3.2 关系类型（Cypher 关键约束）

```cypher
// 唯一约束
CREATE CONSTRAINT project_id IF NOT EXISTS
    FOR (p:Project) REQUIRE p.projectId IS UNIQUE;
CREATE CONSTRAINT module_id IF NOT EXISTS
    FOR (m:Module) REQUIRE m.moduleId IS UNIQUE;
CREATE CONSTRAINT class_id IF NOT EXISTS
    FOR (c:Class) REQUIRE c.classId IS UNIQUE;
CREATE CONSTRAINT method_id IF NOT EXISTS
    FOR (mt:Method) REQUIRE mt.methodId IS UNIQUE;

// 检索索引
CREATE INDEX class_name IF NOT EXISTS FOR (c:Class) ON (c.name);
CREATE INDEX method_name IF NOT EXISTS FOR (mt:Method) ON (mt.name);
CREATE INDEX module_business_tag IF NOT EXISTS FOR (m:Module) ON (m.businessTag);
```

### 3.3 ChromaDB 集合定义

| 集合 | 维度 | metadata 字段 | 用途 |
| ---- | ---- | ---- | ---- |
| `code_semantics` | 1536（OpenAI text-embedding-3-small）| entity_id, entity_type, file_path, module_name, business_tag, project_id | 代码注释 / 业务描述 |
| `project_overview` | 1536 | project_id, update_time | 项目摘要、模块概要 |

---

## 4. 代码解析层实现

### 4.1 解析器接口

```java
public interface CodeParser {

    /** 是否支持指定语言 */
    boolean supports(Language language);

    /** 解析单个文件，返回实体与关系 */
    ParseResult parseFile(Path file);

    /** 解析整个工程目录 */
    ParseResult parseProject(Path projectRoot);
}

public record ParseResult(
        List<ClassNode> classes,
        List<MethodNode> methods,
        List<ModuleNode> modules,
        List<RelationEdge> relations
) {}

public record RelationEdge(
        String fromId,
        String toId,
        RelationType type
) {}
```

### 4.2 JavaParser 封装

```java
@Component
public class JavaAstParser implements CodeParser {

    private final JavaSymbolSolver symbolSolver;

    public JavaAstParser() {
        TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(/* projectRoot */));
        this.symbolSolver = new JavaSymbolSolver(typeSolver);

        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(symbolSolver)
                .setLanguageLevel(LanguageLevel.JAVA_21);
    }

    @Override
    public boolean supports(Language language) {
        return language == Language.JAVA;
    }

    @Override
    public ParseResult parseFile(Path file) {
        CompilationUnit cu = StaticJavaParser.parse(file);
        var classExtractor = new JavaEntityExtractor(file);
        var relExtractor = new JavaRelationExtractor(symbolSolver);

        cu.accept(classExtractor, null);
        cu.accept(relExtractor, null);

        return new ParseResult(
                classExtractor.classes(),
                classExtractor.methods(),
                List.of(),
                relExtractor.relations());
    }

    @Override
    public ParseResult parseProject(Path projectRoot) {
        try (var stream = Files.walk(projectRoot)) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                    .map(this::parseFile)
                    .reduce(ParseResult.empty(), ParseResult::merge);
        } catch (IOException e) {
            throw new ParseException("扫描失败: " + projectRoot, e);
        }
    }
}
```

### 4.3 实体抽取（Visitor）

```java
class JavaEntityExtractor extends VoidVisitorAdapter<Void> {

    private final Path file;
    private final List<ClassNode> classes = new ArrayList<>();
    private final List<MethodNode> methods = new ArrayList<>();

    JavaEntityExtractor(Path file) { this.file = file; }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        ClassNode node = new ClassNode();
        node.setClassId(n.getFullyQualifiedName().orElseThrow());
        node.setName(n.getNameAsString());
        node.setFilePath(file.toString());
        node.setComment(n.getJavadocComment().map(Comment::getContent).orElse(""));
        node.setType(n.isInterface() ? ClassType.INTERFACE : ClassType.CLASS);
        classes.add(node);
        super.visit(n, arg);
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        MethodNode m = new MethodNode();
        n.getParentNode().ifPresent(p -> {
            if (p instanceof ClassOrInterfaceDeclaration c) {
                m.setMethodId(c.getFullyQualifiedName().orElseThrow()
                        + "#" + n.getSignature());
                m.setClassId(c.getFullyQualifiedName().orElseThrow());
            }
        });
        m.setName(n.getNameAsString());
        m.setReturnType(n.getTypeAsString());
        m.setParams(n.getParameters().stream()
                .map(Parameter::toString)
                .collect(Collectors.joining(",")));
        m.setComment(n.getJavadocComment().map(Comment::getContent).orElse(""));
        m.setStartLine(n.getBegin().map(p -> p.line).orElse(-1));
        m.setEndLine(n.getEnd().map(p -> p.line).orElse(-1));
        methods.add(m);
        super.visit(n, arg);
    }

    List<ClassNode> classes() { return classes; }
    List<MethodNode> methods() { return methods; }
}
```

### 4.4 Python 解析（外部进程）

JDK 21 没有原生 Python AST 解析能力，通过 `ProcessBuilder` 调用本地 Python，输出 JSON 后反序列化：

```java
@Component
public class PythonAstParser implements CodeParser {

    private final ObjectMapper mapper;
    private final String pythonBin;

    public PythonAstParser(@Value("${lookgraph.python.bin:python3}") String pythonBin,
                           ObjectMapper mapper) {
        this.pythonBin = pythonBin;
        this.mapper = mapper;
    }

    @Override
    public ParseResult parseFile(Path file) {
        try {
            Process p = new ProcessBuilder(pythonBin,
                    "-m", "lookgraph_pyast", file.toString())
                    .redirectErrorStream(true)
                    .start();
            ParseResult result = mapper.readValue(p.getInputStream(), ParseResult.class);
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new ParseException("Python 解析超时: " + file);
            }
            return result;
        } catch (Exception e) {
            throw new ParseException("Python 解析失败: " + file, e);
        }
    }

    @Override
    public boolean supports(Language language) { return language == Language.PYTHON; }
    // parseProject 略
}
```

---

## 5. 服务层实现

### 5.1 项目初始化

```java
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final List<CodeParser> parsers;
    private final ProjectRepository projectRepository;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;
    private final VectorIndexService vectorIndexService;
    private final SummaryService summaryService;

    @Transactional
    public ProjectInitResponse init(ProjectInitRequest req) {
        Path root = Path.of(req.path());
        Language lang = req.language();

        CodeParser parser = parsers.stream()
                .filter(p -> p.supports(lang))
                .findFirst()
                .orElseThrow(() -> new BizException("不支持的语言: " + lang));

        ParseResult result = parser.parseProject(root);

        ProjectNode project = new ProjectNode();
        project.setProjectId(IdUtil.simpleUuid());
        project.setName(req.name());
        project.setPath(req.path());
        project.setTechStack(lang.name());
        project.setCreateTime(Instant.now());
        projectRepository.save(project);

        classRepository.saveAll(result.classes());
        methodRepository.saveAll(result.methods());

        vectorIndexService.indexEntities(project.getProjectId(), result);
        summaryService.generateAndIndex(project.getProjectId());

        return new ProjectInitResponse(project.getProjectId(),
                result.classes().size(),
                result.methods().size());
    }
}
```

### 5.2 结构查询服务

```java
@Service
public class StructureQueryService {

    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;

    /** 查询类的关联关系（继承、实现、依赖） */
    public ClassRelationView classRelations(String classId) {
        var node = classRepository.findById(classId)
                .orElseThrow(() -> new BizException("class not found: " + classId));
        return ClassRelationView.from(node);
    }

    /** 查询方法调用链路（默认深度 3） */
    public CallChainView callChain(String methodId, int depth) {
        return methodRepository.findCallChain(methodId, depth);
    }

    /** 查询模块下所有类 */
    public List<ClassNode> classesInModule(String moduleId) {
        return classRepository.findByModuleId(moduleId);
    }
}
```

### 5.3 Repository 自定义 Cypher

```java
public interface MethodRepository extends Neo4jRepository<MethodNode, String> {

    @Query("""
            MATCH path = (m:Method {methodId: $methodId})-[:CALLS*1..%d]->(child:Method)
            RETURN path
            """.formatted(/* depth replaced */ 3))
    CallChainView findCallChain(@Param("methodId") String methodId,
                                @Param("depth") int depth);

    @Query("""
            MATCH (m:Method {methodId: $methodId})<-[:CALLS*1..3]-(caller:Method)
            RETURN DISTINCT caller
            """)
    List<MethodNode> findUpstreamCallers(@Param("methodId") String methodId);
}
```

### 5.4 影响范围分析

```java
@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

    private final Neo4jClient neo4jClient;

    public ImpactReport analyze(EntityType type, String entityId) {
        return switch (type) {
            case CLASS -> analyzeClass(entityId);
            case METHOD -> analyzeMethod(entityId);
        };
    }

    private ImpactReport analyzeMethod(String methodId) {
        Collection<Map<String, Object>> rows = neo4jClient
                .query("""
                        MATCH (target:Method {methodId: $id})
                        OPTIONAL MATCH (target)<-[:CALLS*1..5]-(caller:Method)
                        OPTIONAL MATCH (caller)-[:BELONGS_TO]->(c:Class)-[:BELONGS_TO]->(m:Module)
                        RETURN DISTINCT caller, c, m
                        """)
                .bind(methodId).to("id")
                .fetch().all();
        return ImpactReport.fromRows(rows);
    }
}
```

### 5.5 语义检索服务

```java
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingProvider embeddingProvider;
    private final ChromaClient chromaClient;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;

    public List<SemanticHit> search(SemanticSearchRequest req) {
        float[] vector = embeddingProvider.embed(req.query());

        Map<String, Object> filter = new HashMap<>();
        if (StringUtils.hasText(req.module())) {
            filter.put("module_name", req.module());
        }
        if (StringUtils.hasText(req.businessTag())) {
            filter.put("business_tag", req.businessTag());
        }

        QueryResult result = chromaClient.query(
                "code_semantics", vector, req.topK(), filter);

        return result.documents().stream()
                .map(this::enrichWithEntity)
                .toList();
    }

    private SemanticHit enrichWithEntity(VectorDocument doc) {
        String entityType = (String) doc.metadata().get("entity_type");
        String entityId = (String) doc.metadata().get("entity_id");
        Object entity = "method".equals(entityType)
                ? methodRepository.findById(entityId).orElse(null)
                : classRepository.findById(entityId).orElse(null);
        return new SemanticHit(doc.score(), doc.document(), doc.metadata(), entity);
    }
}
```

### 5.6 上下文组装

```java
@Service
@RequiredArgsConstructor
public class ContextAssemblyService {

    private final MethodRepository methodRepository;
    private final ClassRepository classRepository;
    private final SourceCodeReader sourceReader;

    /** 方法精简上下文：源码 + 上下游 1 层 + 注释 + 业务标签 */
    public MethodContext methodContext(String methodId) {
        MethodNode target = methodRepository.findById(methodId)
                .orElseThrow();
        ClassNode owner = classRepository.findById(target.getClassId()).orElseThrow();

        List<MethodNode> upstream = methodRepository.findUpstreamCallers(methodId)
                .stream().limit(5).toList();
        List<MethodNode> downstream = target.getCalls().stream().limit(5).toList();

        String sourceSnippet = sourceReader.readRange(
                Path.of(owner.getFilePath()), target.getStartLine(), target.getEndLine());

        return new MethodContext(
                target,
                sourceSnippet,
                owner,
                upstream,
                downstream,
                target.getComment());
    }
}
```

---

## 6. 增量更新与文件监听

### 6.1 文件监听器

JDK 21 支持虚拟线程，每个项目使用一个虚拟线程监听 `WatchService`，资源开销极低：

```java
@Component
@RequiredArgsConstructor
public class FileWatcher implements DisposableBean {

    private final IncrementalScanner incrementalScanner;
    private final Map<String, Thread> watchers = new ConcurrentHashMap<>();

    public void watch(String projectId, Path projectRoot) {
        Thread thread = Thread.ofVirtual()
                .name("lookgraph-watch-" + projectId)
                .start(() -> doWatch(projectId, projectRoot));
        watchers.put(projectId, thread);
    }

    private void doWatch(String projectId, Path root) {
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            registerRecursive(root, ws);
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.take();
                List<Path> changed = key.pollEvents().stream()
                        .map(e -> ((Path) key.watchable()).resolve((Path) e.context()))
                        .toList();
                key.reset();
                if (!changed.isEmpty()) {
                    incrementalScanner.scan(projectId, changed);
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        watchers.values().forEach(Thread::interrupt);
    }
}
```

### 6.2 增量扫描

```java
@Service
@RequiredArgsConstructor
public class IncrementalScanner {

    private final List<CodeParser> parsers;
    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;
    private final VectorIndexService vectorIndexService;

    @Async("virtualThreadExecutor")
    @Transactional
    public void scan(String projectId, List<Path> changedFiles) {
        for (Path file : changedFiles) {
            if (Files.notExists(file)) {
                deleteByFile(file);
                continue;
            }
            CodeParser parser = pickParser(file);
            ParseResult result = parser.parseFile(file);

            classRepository.deleteAllByFilePath(file.toString());
            classRepository.saveAll(result.classes());
            methodRepository.saveAll(result.methods());

            vectorIndexService.upsert(projectId, result);
        }
    }
}
```

### 6.3 虚拟线程 Executor

```java
@Configuration
public class AsyncConfig {

    @Bean(name = "virtualThreadExecutor")
    public AsyncTaskExecutor virtualThreadExecutor() {
        return new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
        return handler -> handler.setExecutor(
                Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

---

## 7. ChromaDB 集成

### 7.1 客户端配置

```java
@Configuration
public class ChromaConfig {

    @Bean
    public Client chromaClient(@Value("${lookgraph.chroma.url}") String url) {
        return new Client(url);
    }

    @Bean
    public Collection codeSemanticsCollection(Client client) throws Exception {
        return client.createCollection(
                "code_semantics",
                Map.of("hnsw:space", "cosine"),
                true,
                null);
    }

    @Bean
    public Collection projectOverviewCollection(Client client) throws Exception {
        return client.createCollection("project_overview", Map.of(), true, null);
    }
}
```

### 7.2 向量索引服务

```java
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    @Qualifier("codeSemanticsCollection")
    private final Collection codeSemantics;
    private final EmbeddingProvider embeddingProvider;

    public void indexEntities(String projectId, ParseResult result) {
        List<String> ids = new ArrayList<>();
        List<String> docs = new ArrayList<>();
        List<Map<String, Object>> metas = new ArrayList<>();

        result.classes().forEach(c -> {
            if (StringUtils.hasText(c.getComment())) {
                ids.add(c.getClassId());
                docs.add(c.getComment());
                metas.add(Map.of(
                        "entity_id", c.getClassId(),
                        "entity_type", "class",
                        "file_path", c.getFilePath(),
                        "module_name", Optional.ofNullable(c.getModuleId()).orElse(""),
                        "project_id", projectId));
            }
        });
        result.methods().forEach(m -> { /* 同上 */ });

        List<List<Float>> embeddings = embeddingProvider.embedBatch(docs);
        codeSemantics.add(embeddings, metas, docs, ids);
    }
}
```

### 7.3 Embedding Provider

```java
public interface EmbeddingProvider {
    float[] embed(String text);
    List<List<Float>> embedBatch(List<String> texts);
}

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;     // Spring AI

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        var response = embeddingModel.embedForResponse(texts);
        return response.getResults().stream()
                .map(r -> Floats.asList(r.getOutput()))
                .toList();
    }
}
```

> 也可替换为本地 BGE / m3e 模型，仅需新增 `LocalEmbeddingProvider` 实现该接口并通过 Spring Profile 切换。

---

## 8. REST API 实现

### 8.1 控制器示例

```java
@RestController
@RequestMapping("/api/project")
@Tag(name = "项目管理")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SummaryService summaryService;

    @PostMapping("/init")
    public Result<ProjectInitResponse> init(@RequestBody @Valid ProjectInitRequest req) {
        return Result.ok(projectService.init(req));
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestParam String projectId) {
        projectService.triggerIncrementalUpdate(projectId);
        return Result.ok();
    }

    @GetMapping("/summary")
    public Result<ProjectSummary> summary(@RequestParam String projectId) {
        return Result.ok(summaryService.get(projectId));
    }
}
```

```java
@RestController
@RequestMapping("/api/structure")
@RequiredArgsConstructor
public class StructureController {

    private final StructureQueryService structureService;
    private final ImpactAnalysisService impactService;

    @GetMapping("/class/{classId}/relation")
    public Result<ClassRelationView> classRelation(@PathVariable String classId) {
        return Result.ok(structureService.classRelations(classId));
    }

    @GetMapping("/method/{methodId}/callchain")
    public Result<CallChainView> callChain(@PathVariable String methodId,
                                           @RequestParam(defaultValue = "3") int depth) {
        return Result.ok(structureService.callChain(methodId, depth));
    }

    @GetMapping("/module/{moduleId}/classes")
    public Result<List<ClassNode>> classesInModule(@PathVariable String moduleId) {
        return Result.ok(structureService.classesInModule(moduleId));
    }

    @GetMapping("/impact/{entityType}/{entityId}")
    public Result<ImpactReport> impact(@PathVariable EntityType entityType,
                                       @PathVariable String entityId) {
        return Result.ok(impactService.analyze(entityType, entityId));
    }
}
```

```java
@RestController
@RequestMapping("/api/semantic")
@RequiredArgsConstructor
public class SemanticController {

    private final SemanticSearchService semanticSearchService;

    @PostMapping("/search")
    public Result<List<SemanticHit>> search(@RequestBody @Valid SemanticSearchRequest req) {
        return Result.ok(semanticSearchService.search(req));
    }
}
```

```java
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextAssemblyService contextService;

    @GetMapping("/method/{methodId}")
    public Result<MethodContext> methodContext(@PathVariable String methodId) {
        return Result.ok(contextService.methodContext(methodId));
    }

    @GetMapping("/class/{classId}")
    public Result<ClassContext> classContext(@PathVariable String classId) {
        return Result.ok(contextService.classContext(classId));
    }
}
```

### 8.2 统一响应与异常

```java
public record Result<T>(int code, String message, T data) {
    public static <T> Result<T> ok(T data) { return new Result<>(0, "ok", data); }
    public static <T> Result<T> ok() { return new Result<>(0, "ok", null); }
    public static <T> Result<T> fail(int code, String msg) { return new Result<>(code, msg, null); }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handle(BizException e) {
        return ResponseEntity.badRequest().body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handle(Exception e) {
        log.error("unhandled error", e);
        return ResponseEntity.internalServerError().body(Result.fail(500, e.getMessage()));
    }
}
```

### 8.3 DTO（record）

```java
public record ProjectInitRequest(
        @NotBlank String name,
        @NotBlank String path,
        @NotNull Language language) {}

public record ProjectInitResponse(String projectId, int classCount, int methodCount) {}

public record SemanticSearchRequest(
        @NotBlank String query,
        String module,
        String businessTag,
        @Min(1) @Max(50) int topK) {}
```

---

## 9. 配置文件

### 9.1 application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: look-graph
  threads:
    virtual:
      enabled: true                        # 启用虚拟线程
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD:neo4j}
    pool:
      max-connection-pool-size: 50
      connection-acquisition-timeout: 60s
  data:
    neo4j:
      database: neo4j
      auto-index: validate
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small

lookgraph:
  chroma:
    url: http://localhost:8000
  python:
    bin: python3
  scanner:
    excludes:
      - "**/target/**"
      - "**/build/**"
      - "**/node_modules/**"
      - "**/.git/**"
    extensions:
      java: [".java"]
      python: [".py"]
      javascript: [".js", ".ts"]

logging:
  level:
    com.lookgraph: INFO
    org.springframework.data.neo4j: WARN

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### 9.2 不同环境

- `application-dev.yml`：本地 Neo4j、ChromaDB
- `application-prod.yml`：生产环境，关闭 Swagger，启用监控
- 通过 `SPRING_PROFILES_ACTIVE` 切换

---

## 10. 部署方案

### 10.1 中间件 Docker Compose

```yaml
version: "3.9"
services:
  neo4j:
    image: neo4j:5.20-community
    ports: ["7474:7474", "7687:7687"]
    environment:
      NEO4J_AUTH: neo4j/lookgraph123
      NEO4J_PLUGINS: '["apoc"]'
      NEO4J_dbms_memory_heap_max__size: 2G
    volumes:
      - neo4j_data:/data

  chromadb:
    image: chromadb/chroma:0.5.5
    ports: ["8000:8000"]
    volumes:
      - chroma_data:/chroma/chroma

  lookgraph:
    image: lookgraph:1.0.0
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_NEO4J_URI: bolt://neo4j:7687
      NEO4J_PASSWORD: lookgraph123
      LOOKGRAPH_CHROMA_URL: http://chromadb:8000
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on: [neo4j, chromadb]

volumes:
  neo4j_data:
  chroma_data:
```

### 10.2 Dockerfile（多阶段构建）

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/look-graph-*.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational --enable-preview"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 10.3 资源建议

| 项目规模 | 文件数 | JVM 内存 | Neo4j 堆 | 建议配置 |
| ---- | ---- | ---- | ---- | ---- |
| 小型 | < 1k | 1G | 1G | 单机 4 核 8G |
| 中型 | 1k ~ 10k | 2G | 2G | 单机 8 核 16G |
| 大型 | 10k+ | 4G | 4G | 16 核 32G + 独立 Neo4j |

---

## 11. 性能与可观测性

### 11.1 性能策略

1. **Cypher 批量写入**：`UNWIND $rows AS row CREATE ...`，单事务 1000 节点/批。
2. **向量批处理**：Embedding 单批 64 条，减少 OpenAI 调用次数与延迟。
3. **虚拟线程**：Web、文件监听、增量扫描全部使用虚拟线程，吞吐与并发显著提升。
4. **本地缓存**：`Caffeine` 缓存项目摘要、热点类的结构关系，TTL 5 分钟。
5. **G1 → ZGC**：JDK 21 启用 ZGC + Generational ZGC，停顿 < 1ms。

### 11.2 监控

- Spring Boot Actuator：`/actuator/health`、`/actuator/metrics`
- Micrometer + Prometheus：JVM、HTTP、Neo4j 连接池指标
- 自定义业务埋点：`@Timed("lookgraph.parse")`、`@Counted("lookgraph.semantic.hit")`

---

## 12. 测试策略

### 12.1 测试金字塔

| 层级 | 范围 | 工具 |
| ---- | ---- | ---- |
| 单元测试 | Parser、工具类、纯函数 | JUnit 5 + Mockito |
| 切片测试 | Service、Repository | `@DataNeo4jTest` |
| 集成测试 | Controller + Neo4j + Chroma | Testcontainers |
| 端到端 | 真实工程 | 自定义 Shell |

### 12.2 Testcontainers 集成

```java
@Testcontainers
@SpringBootTest
class StructureQueryIT {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.20")
            .withAdminPassword("test1234");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.password", () -> "test1234");
    }

    @Autowired StructureQueryService service;

    @Test
    void shouldQueryCallChain() { /* ... */ }
}
```

---

## 13. 安全设计

| 风险 | 控制措施 |
| ---- | ---- |
| 任意路径解析 | `Path.normalize()` + 白名单校验，禁止访问 `~`、`/etc` 等敏感路径 |
| Cypher 注入 | 全部使用参数化查询（`$param`），禁止字符串拼接 |
| 敏感配置泄露 | 密码、API Key 走环境变量；`actuator` 端点关闭敏感信息 |
| 解析超长文件 | 单文件 ≤ 5MB；进程级超时 30s |
| 多租户隔离 | 所有查询带 `projectId` 过滤；Chroma metadata 强制写入 `project_id` |

---

## 14. 演进路线（与产品文档 V2/V3 对齐）

### V1.0（MVP，本文重点）
- Java AST 全量 / 增量解析
- Neo4j 结构图谱、ChromaDB 语义检索
- 完整 REST API、Swagger 文档

### V2.0
- Python / JavaScript / Go 解析适配（新增 `CodeParser` 实现）
- 文件监听全自动增量
- 业务标签识别（基于 LLM 提示词分类）

### V3.0
- 调用链可视化（前端 + Cytoscape.js）
- 多项目并行管理（项目隔离 + 路由）
- Claude 原生插件封装（Anthropic Tools 协议）

---

## 15. 关键风险与对策

| 风险 | 影响 | 对策 |
| ---- | ---- | ---- |
| Spring Boot 4.0.6 与 JDK 21 兼容性 | 启动失败 | 使用 Eclipse Temurin 21 LTS；锁定依赖版本 |
| 大型项目 OOM | 解析中断 | 分批解析、流式处理、JVM `-Xmx4g` |
| Embedding 调用费用 | 成本失控 | 仅对带注释的实体向量化；可切换本地模型 |
| Neo4j 5.x 写入瓶颈 | 初始化慢 | 批量写入 + 索引延迟创建（先建节点再建索引）|
| 跨语言解析一致性 | 实体 ID 冲突 | 统一 ID 规则：`<lang>:<projectId>:<fullyQualifiedName>` |
