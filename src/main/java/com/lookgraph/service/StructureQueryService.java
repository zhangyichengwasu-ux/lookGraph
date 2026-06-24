package com.lookgraph.service;

import com.lookgraph.common.exception.BizException;
import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.repository.neo4j.ClassRepository;
import com.lookgraph.domain.repository.neo4j.MethodRepository;
import com.lookgraph.dto.response.CallChainView;
import com.lookgraph.dto.response.ClassRelationView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StructureQueryService {

    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;

    public ClassRelationView classRelations(String classId) {
        ClassNode node = classRepository.findById(classId)
                .orElseThrow(() -> new BizException("类不存在: " + classId));

        var projection = classRepository.findClassRelations(classId);
        if (projection == null) {
            return ClassRelationView.empty(node);
        }

        return new ClassRelationView(
                projection.getC(),
                projection.getParent(),
                projection.getInterfaces(),
                projection.getDependencies(),
                projection.getDependedBy());
    }

    public CallChainView callChain(String methodId) {
        MethodNode target = methodRepository.findById(methodId)
                .orElseThrow(() -> new BizException("方法不存在: " + methodId));

        List<MethodNode> upstream = methodRepository.findUpstreamCallers(methodId);
        List<MethodNode> downstream = methodRepository.findDownstreamCallees(methodId);

        return new CallChainView(target, upstream, downstream);
    }

    public List<ClassNode> classesInModule(String moduleId) {
        return classRepository.findByModuleId(moduleId);
    }

    public List<MethodNode> methodsInClass(String classId) {
        return methodRepository.findByClassId(classId);
    }
}
