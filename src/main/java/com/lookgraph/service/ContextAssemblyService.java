package com.lookgraph.service;

import com.lookgraph.common.exception.BizException;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.repository.ClassRepository;
import com.lookgraph.domain.repository.MethodRepository;
import com.lookgraph.dto.response.ClassContext;
import com.lookgraph.dto.response.MethodContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAssemblyService {

    private final MethodRepository methodRepository;
    private final ClassRepository classRepository;

    public MethodContext methodContext(String methodId) {
        MethodNode target = methodRepository.findById(methodId)
                .orElseThrow(() -> new BizException("方法不存在: " + methodId));

        ClassNode owner = classRepository.findById(target.getClassId())
                .orElse(null);

        List<MethodNode> upstream = methodRepository.findUpstreamCallers(methodId)
                .stream().limit(5).toList();
        List<MethodNode> downstream = methodRepository.findDownstreamCallees(methodId)
                .stream().limit(5).toList();

        String sourceSnippet = "";
        if (owner != null && owner.getFilePath() != null) {
            sourceSnippet = readSourceRange(
                    Path.of(owner.getFilePath()),
                    target.getStartLine(),
                    target.getEndLine());
        }

        return new MethodContext(
                target,
                sourceSnippet,
                owner,
                upstream,
                downstream,
                target.getComment());
    }

    public ClassContext classContext(String classId) {
        ClassNode classNode = classRepository.findById(classId)
                .orElseThrow(() -> new BizException("类不存在: " + classId));

        List<MethodNode> methods = methodRepository.findByClassId(classId);

        String sourceSnippet = "";
        if (classNode.getFilePath() != null) {
            sourceSnippet = readFullFile(Path.of(classNode.getFilePath()));
        }

        return new ClassContext(
                classNode,
                sourceSnippet,
                methods,
                classNode.getParent(),
                classNode.getInterfaces() != null ? classNode.getInterfaces().stream().toList() : List.of(),
                classNode.getComment());
    }

    private String readSourceRange(Path file, int startLine, int endLine) {
        if (startLine <= 0 || endLine <= 0) return "";
        try {
            List<String> lines = Files.readAllLines(file);
            int start = Math.max(0, startLine - 1);
            int end = Math.min(lines.size(), endLine);
            return String.join("\n", lines.subList(start, end));
        } catch (IOException e) {
            log.warn("读取源码失败: {}", file, e);
            return "";
        }
    }

    private String readFullFile(Path file) {
        try {
            String content = Files.readString(file);
            if (content.length() > 10000) {
                return content.substring(0, 10000) + "\n// ... 文件过长，已截断";
            }
            return content;
        } catch (IOException e) {
            log.warn("读取文件失败: {}", file, e);
            return "";
        }
    }
}
