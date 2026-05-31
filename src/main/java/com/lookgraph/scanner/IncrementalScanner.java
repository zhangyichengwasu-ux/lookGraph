package com.lookgraph.scanner;

import com.lookgraph.common.enums.Language;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.repository.ClassRepository;
import com.lookgraph.domain.repository.MethodRepository;
import com.lookgraph.parser.CodeParser;
import com.lookgraph.parser.ParseResult;
import com.lookgraph.vector.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
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
        log.info("增量扫描: {} 个文件变更", changedFiles.size());

        for (Path file : changedFiles) {
            try {
                if (Files.notExists(file)) {
                    handleDelete(file);
                    continue;
                }
                handleUpsert(projectId, file);
            } catch (Exception e) {
                log.warn("增量扫描文件失败: {}", file, e);
            }
        }
    }

    private void handleDelete(Path file) {
        String filePath = file.toString();
        classRepository.deleteAllByFilePath(filePath);
        vectorIndexService.deleteByFile(filePath);
        log.debug("已删除文件相关数据: {}", filePath);
    }

    private void handleUpsert(String projectId, Path file) {
        Language lang = detectLanguageByFile(file);
        CodeParser parser = parsers.stream()
                .filter(p -> p.supports(lang))
                .findFirst()
                .orElse(null);

        if (parser == null) {
            return;
        }

        ParseResult result = parser.parseFile(file);

        classRepository.deleteAllByFilePath(file.toString());
        result.classes().forEach(c -> c.setProjectId(projectId));
        result.methods().forEach(m -> m.setProjectId(projectId));
        classRepository.saveAll(result.classes());
        methodRepository.saveAll(result.methods());

        vectorIndexService.upsert(projectId, result);
    }

    private Language detectLanguageByFile(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".java")) return Language.JAVA;
        if (name.endsWith(".py")) return Language.PYTHON;
        if (name.endsWith(".js") || name.endsWith(".ts")) return Language.JAVASCRIPT;
        return Language.JAVA;
    }
}
