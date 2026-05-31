package com.lookgraph.scanner;

import com.lookgraph.common.enums.Language;
import com.lookgraph.parser.CodeParser;
import com.lookgraph.parser.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectScanner {

    private final List<CodeParser> parsers;

    public ParseResult fullScan(Path projectRoot, Language language) {
        CodeParser parser = parsers.stream()
                .filter(p -> p.supports(language))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的语言: " + language));

        log.info("开始全量扫描: {} ({})", projectRoot, language);
        ParseResult result = parser.parseProject(projectRoot);
        log.info("全量扫描完成: {} 个类, {} 个方法, {} 个模块",
                result.classes().size(), result.methods().size(), result.modules().size());
        return result;
    }

    public Language detectLanguage(Path projectRoot) {
        if (projectRoot.resolve("pom.xml").toFile().exists() ||
                projectRoot.resolve("build.gradle").toFile().exists()) {
            return Language.JAVA;
        }
        if (projectRoot.resolve("requirements.txt").toFile().exists() ||
                projectRoot.resolve("setup.py").toFile().exists() ||
                projectRoot.resolve("pyproject.toml").toFile().exists()) {
            return Language.PYTHON;
        }
        if (projectRoot.resolve("package.json").toFile().exists()) {
            return Language.JAVASCRIPT;
        }
        return Language.JAVA;
    }
}
