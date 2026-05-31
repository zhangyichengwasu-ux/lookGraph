package com.lookgraph.parser.python;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lookgraph.common.enums.Language;
import com.lookgraph.common.exception.ParseException;
import com.lookgraph.config.ScannerProperties;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.parser.CodeParser;
import com.lookgraph.parser.ParseResult;
import com.lookgraph.parser.RelationEdge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
public class PythonAstParser implements CodeParser {

    private final String pythonBin;
    private final ObjectMapper objectMapper;
    private final List<String> excludePatterns;

    public PythonAstParser(@Value("${lookgraph.python.bin}") String pythonBin,
                           ObjectMapper objectMapper,
                           ScannerProperties scannerProperties) {
        this.pythonBin = pythonBin;
        this.objectMapper = objectMapper;
        this.excludePatterns = scannerProperties.getExcludes();
    }

    @Override
    public boolean supports(Language language) {
        return language == Language.PYTHON;
    }

    @Override
    public ParseResult parseFile(Path file) {
        try {
            Path script = createParserScript();
            Process process = new ProcessBuilder(pythonBin, script.toString(), file.toString())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ParseException("Python 解析超时: " + file);
            }

            if (process.exitValue() != 0) {
                log.warn("Python 解析失败: {} - {}", file, output);
                return ParseResult.empty();
            }

            return parsePythonOutput(output, file);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("Python 解析失败: " + file, e);
        }
    }

    @Override
    public ParseResult parseProject(Path projectRoot) {
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            List<Path> pyFiles = stream
                    .filter(p -> p.toString().endsWith(".py"))
                    .filter(p -> !isExcluded(p, projectRoot))
                    .toList();

            log.info("扫描到 {} 个 Python 文件", pyFiles.size());

            ParseResult result = ParseResult.empty();
            for (Path file : pyFiles) {
                try {
                    result = result.merge(parseFile(file));
                } catch (Exception e) {
                    log.warn("跳过解析失败的文件: {}", file, e);
                }
            }
            return result;
        } catch (IOException e) {
            throw new ParseException("扫描 Python 项目失败: " + projectRoot, e);
        }
    }

    @SuppressWarnings("unchecked")
    private ParseResult parsePythonOutput(String output, Path file) throws Exception {
        Map<String, Object> data = objectMapper.readValue(output, Map.class);
        List<Map<String, Object>> classesData = (List<Map<String, Object>>) data.getOrDefault("classes", List.of());
        List<Map<String, Object>> methodsData = (List<Map<String, Object>>) data.getOrDefault("methods", List.of());

        List<ClassNode> classes = classesData.stream().map(c -> {
            ClassNode node = new ClassNode();
            node.setClassId((String) c.get("id"));
            node.setName((String) c.get("name"));
            node.setFilePath(file.toString());
            node.setComment((String) c.getOrDefault("comment", ""));
            return node;
        }).toList();

        List<MethodNode> methods = methodsData.stream().map(m -> {
            MethodNode node = new MethodNode();
            node.setMethodId((String) m.get("id"));
            node.setName((String) m.get("name"));
            node.setClassId((String) m.getOrDefault("class_id", ""));
            node.setComment((String) m.getOrDefault("comment", ""));
            node.setStartLine((int) m.getOrDefault("start_line", -1));
            node.setEndLine((int) m.getOrDefault("end_line", -1));
            return node;
        }).toList();

        return new ParseResult(classes, methods, List.of(), List.of());
    }

    private Path createParserScript() throws IOException {
        Path script = Files.createTempFile("lookgraph_pyast_", ".py");
        Files.writeString(script, PYTHON_PARSER_SCRIPT);
        script.toFile().deleteOnExit();
        return script;
    }

    private boolean isExcluded(Path file, Path root) {
        String relative = root.relativize(file).toString();
        return excludePatterns.stream().anyMatch(pattern -> {
            String regex = pattern.replace("**/", "(.*/)?").replace("/**", "(/.*)?").replace("*", "[^/]*");
            return relative.matches(regex);
        });
    }

    private static final String PYTHON_PARSER_SCRIPT = """
            import ast
            import sys
            import json

            def parse_file(filepath):
                with open(filepath, 'r', encoding='utf-8') as f:
                    source = f.read()
                tree = ast.parse(source, filename=filepath)
                classes = []
                methods = []
                module_name = filepath.replace('/', '.').replace('.py', '')

                for node in ast.walk(tree):
                    if isinstance(node, ast.ClassDef):
                        class_id = f"{module_name}.{node.name}"
                        classes.append({
                            "id": class_id,
                            "name": node.name,
                            "comment": ast.get_docstring(node) or "",
                            "start_line": node.lineno,
                            "end_line": node.end_lineno or node.lineno
                        })
                        for item in node.body:
                            if isinstance(item, ast.FunctionDef) or isinstance(item, ast.AsyncFunctionDef):
                                method_id = f"{class_id}#{item.name}"
                                methods.append({
                                    "id": method_id,
                                    "name": item.name,
                                    "class_id": class_id,
                                    "comment": ast.get_docstring(item) or "",
                                    "start_line": item.lineno,
                                    "end_line": item.end_lineno or item.lineno
                                })
                    elif isinstance(node, ast.FunctionDef) or isinstance(node, ast.AsyncFunctionDef):
                        if not any(isinstance(p, ast.ClassDef) for p in ast.walk(tree)):
                            method_id = f"{module_name}#{node.name}"
                            methods.append({
                                "id": method_id,
                                "name": node.name,
                                "class_id": "",
                                "comment": ast.get_docstring(node) or "",
                                "start_line": node.lineno,
                                "end_line": node.end_lineno or node.lineno
                            })

                return json.dumps({"classes": classes, "methods": methods})

            if __name__ == "__main__":
                print(parse_file(sys.argv[1]))
            """;
}
