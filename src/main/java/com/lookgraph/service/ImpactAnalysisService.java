package com.lookgraph.service;

import com.lookgraph.common.enums.EntityType;
import com.lookgraph.common.exception.BizException;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.node.ModuleNode;
import com.lookgraph.domain.repository.neo4j.ClassRepository;
import com.lookgraph.domain.repository.neo4j.MethodRepository;
import com.lookgraph.domain.repository.neo4j.ModuleRepository;
import com.lookgraph.dto.response.ImpactReport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

    private final Neo4jClient neo4jClient;
    private final MethodRepository methodRepository;
    private final ClassRepository classRepository;
    private final ModuleRepository moduleRepository;

    public ImpactReport analyze(EntityType type, String entityId) {
        return switch (type) {
            case METHOD -> analyzeMethod(entityId);
            case CLASS -> analyzeClass(entityId);
            case MODULE -> analyzeModule(entityId);
        };
    }

    private ImpactReport analyzeMethod(String methodId) {
        MethodNode target = methodRepository.findById(methodId)
                .orElseThrow(() -> new BizException("方法不存在: " + methodId));

        List<MethodNode> callers = methodRepository.findUpstreamCallers(methodId);

        Set<String> classIds = new HashSet<>();
        callers.forEach(m -> {
            if (m.getClassId() != null) classIds.add(m.getClassId());
        });
        if (target.getClassId() != null) classIds.add(target.getClassId());

        List<ClassNode> affectedClasses = classIds.stream()
                .map(classRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Set<String> moduleIds = new HashSet<>();
        affectedClasses.forEach(c -> {
            if (c.getModuleId() != null) moduleIds.add(c.getModuleId());
        });

        List<ModuleNode> affectedModules = moduleIds.stream()
                .map(moduleRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new ImpactReport(methodId, "METHOD", callers, affectedClasses, affectedModules);
    }

    private ImpactReport analyzeClass(String classId) {
        ClassNode target = classRepository.findById(classId)
                .orElseThrow(() -> new BizException("类不存在: " + classId));

        List<MethodNode> methods = methodRepository.findByClassId(classId);
        Set<MethodNode> allCallers = new HashSet<>();
        for (MethodNode m : methods) {
            allCallers.addAll(methodRepository.findUpstreamCallers(m.getMethodId()));
        }

        Set<String> classIds = new HashSet<>();
        allCallers.forEach(m -> {
            if (m.getClassId() != null) classIds.add(m.getClassId());
        });
        classIds.remove(classId);

        List<ClassNode> affectedClasses = classIds.stream()
                .map(classRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new ImpactReport(classId, "CLASS", new ArrayList<>(allCallers), affectedClasses, List.of());
    }

    private ImpactReport analyzeModule(String moduleId) {
        List<ClassNode> classes = classRepository.findByModuleId(moduleId);
        return new ImpactReport(moduleId, "MODULE", List.of(), classes, List.of());
    }
}
