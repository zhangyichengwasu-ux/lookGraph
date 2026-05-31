package com.lookgraph.parser.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.lookgraph.common.enums.ClassType;
import com.lookgraph.common.enums.Language;
import com.lookgraph.common.enums.RelationType;
import com.lookgraph.common.exception.ParseException;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.node.ModuleNode;
import com.lookgraph.parser.CodeParser;
import com.lookgraph.parser.ParseResult;
import com.lookgraph.parser.RelationEdge;
import com.lookgraph.config.ScannerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class JavaAstParser implements CodeParser {

    private final List<String> excludePatterns;

    public JavaAstParser(ScannerProperties scannerProperties) {
        this.excludePatterns = scannerProperties.getExcludes();
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    @Override
    public boolean supports(Language language) {
        return language == Language.JAVA;
    }

    @Override
    public ParseResult parseFile(Path file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            List<ClassNode> classes = new ArrayList<>();
            List<MethodNode> methods = new ArrayList<>();
            List<RelationEdge> relations = new ArrayList<>();

            cu.accept(new ClassVisitor(file, packageName, classes, methods, relations), null);

            return new ParseResult(classes, methods, List.of(), relations);
        } catch (IOException e) {
            throw new ParseException("解析文件失败: " + file, e);
        }
    }

    @Override
    public ParseResult parseProject(Path projectRoot) {
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            List<Path> javaFiles = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !isExcluded(p, projectRoot))
                    .toList();

            log.info("扫描到 {} 个 Java 文件", javaFiles.size());

            ParseResult result = ParseResult.empty();
            for (Path file : javaFiles) {
                try {
                    result = result.merge(parseFile(file));
                } catch (Exception e) {
                    log.warn("跳过解析失败的文件: {}", file, e);
                }
            }

            List<ModuleNode> modules = detectModules(projectRoot);
            return new ParseResult(result.classes(), result.methods(), modules, result.relations());
        } catch (IOException e) {
            throw new ParseException("扫描项目失败: " + projectRoot, e);
        }
    }

    private boolean isExcluded(Path file, Path root) {
        String relative = root.relativize(file).toString();
        return excludePatterns.stream().anyMatch(pattern -> {
            String regex = pattern.replace("**/", "(.*/)?").replace("/**", "(/.*)?").replace("*", "[^/]*");
            return relative.matches(regex);
        });
    }

    private List<ModuleNode> detectModules(Path projectRoot) {
        List<ModuleNode> modules = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(projectRoot, 3)) {
            stream.filter(p -> p.getFileName().toString().equals("pom.xml") ||
                            p.getFileName().toString().equals("build.gradle"))
                    .forEach(p -> {
                        ModuleNode module = new ModuleNode();
                        module.setModuleId(p.getParent().getFileName().toString());
                        module.setName(p.getParent().getFileName().toString());
                        modules.add(module);
                    });
        } catch (IOException e) {
            log.warn("模块检测失败", e);
        }
        return modules;
    }

    private static class ClassVisitor extends VoidVisitorAdapter<Void> {

        private final Path file;
        private final String packageName;
        private final List<ClassNode> classes;
        private final List<MethodNode> methods;
        private final List<RelationEdge> relations;

        ClassVisitor(Path file, String packageName, List<ClassNode> classes,
                     List<MethodNode> methods, List<RelationEdge> relations) {
            this.file = file;
            this.packageName = packageName;
            this.classes = classes;
            this.methods = methods;
            this.relations = relations;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            String fqn = packageName.isEmpty() ? n.getNameAsString() : packageName + "." + n.getNameAsString();

            ClassNode classNode = new ClassNode();
            classNode.setClassId(fqn);
            classNode.setName(n.getNameAsString());
            classNode.setFilePath(file.toString());
            classNode.setComment(n.getJavadocComment().map(Comment::getContent).orElse(""));
            classNode.setType(n.isInterface() ? ClassType.INTERFACE : ClassType.CLASS);
            classes.add(classNode);

            // 继承关系
            n.getExtendedTypes().forEach(ext -> {
                String parentFqn = resolveType(ext);
                relations.add(new RelationEdge(fqn, parentFqn, RelationType.EXTENDS));
            });

            // 实现关系
            n.getImplementedTypes().forEach(impl -> {
                String ifaceFqn = resolveType(impl);
                relations.add(new RelationEdge(fqn, ifaceFqn, RelationType.IMPLEMENTS));
            });

            // 方法
            n.getMethods().forEach(m -> visitMethod(m, fqn));

            super.visit(n, arg);
        }

        @Override
        public void visit(EnumDeclaration n, Void arg) {
            String fqn = packageName.isEmpty() ? n.getNameAsString() : packageName + "." + n.getNameAsString();
            ClassNode classNode = new ClassNode();
            classNode.setClassId(fqn);
            classNode.setName(n.getNameAsString());
            classNode.setFilePath(file.toString());
            classNode.setComment(n.getJavadocComment().map(Comment::getContent).orElse(""));
            classNode.setType(ClassType.ENUM);
            classes.add(classNode);
            super.visit(n, arg);
        }

        @Override
        public void visit(RecordDeclaration n, Void arg) {
            String fqn = packageName.isEmpty() ? n.getNameAsString() : packageName + "." + n.getNameAsString();
            ClassNode classNode = new ClassNode();
            classNode.setClassId(fqn);
            classNode.setName(n.getNameAsString());
            classNode.setFilePath(file.toString());
            classNode.setComment(n.getJavadocComment().map(Comment::getContent).orElse(""));
            classNode.setType(ClassType.RECORD);
            classes.add(classNode);
            super.visit(n, arg);
        }

        private void visitMethod(MethodDeclaration m, String classFqn) {
            String signature = m.getNameAsString() + "(" +
                    m.getParameters().stream()
                            .map(p -> p.getTypeAsString())
                            .collect(Collectors.joining(",")) + ")";
            String methodId = classFqn + "#" + signature;

            MethodNode methodNode = new MethodNode();
            methodNode.setMethodId(methodId);
            methodNode.setName(m.getNameAsString());
            methodNode.setReturnType(m.getTypeAsString());
            methodNode.setParams(m.getParameters().stream()
                    .map(Parameter::toString)
                    .collect(Collectors.joining(", ")));
            methodNode.setComment(m.getJavadocComment().map(Comment::getContent).orElse(""));
            methodNode.setClassId(classFqn);
            methodNode.setStartLine(m.getBegin().map(p -> p.line).orElse(-1));
            methodNode.setEndLine(m.getEnd().map(p -> p.line).orElse(-1));
            methods.add(methodNode);

            // 方法调用关系
            m.findAll(MethodCallExpr.class).forEach(call -> {
                String calleeName = call.getNameAsString();
                String calleeId = classFqn + "#" + calleeName + "(*)";
                relations.add(new RelationEdge(methodId, calleeId, RelationType.CALLS));
            });
        }

        private String resolveType(ClassOrInterfaceType type) {
            return packageName + "." + type.getNameAsString();
        }
    }
}
