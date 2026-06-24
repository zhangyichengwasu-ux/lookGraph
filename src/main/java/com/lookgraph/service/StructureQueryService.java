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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StructureQueryService {

    private final ClassRepository classRepository;
    private final MethodRepository methodRepository;

    public ClassRelationView classRelations(String classId) {
        ClassNode node = classRepository.findById(classId)
                .orElseThrow(() -> new BizException("类不存在: " + classId));

        // 直接查询关联节点
        ClassNode parent = classRepository.findParent(classId);
        List<ClassNode> interfaces = classRepository.findInterfaces(classId);
        List<ClassNode> dependencies = classRepository.findDependencies(classId);
        List<ClassNode> dependedBy = classRepository.findDependedBy(classId);

        return new ClassRelationView(node, parent, interfaces, dependencies, dependedBy);
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
