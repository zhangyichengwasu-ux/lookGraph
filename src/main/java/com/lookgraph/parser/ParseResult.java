package com.lookgraph.parser;

import com.lookgraph.domain.node.ClassNode;
import com.lookgraph.domain.node.MethodNode;
import com.lookgraph.domain.node.ModuleNode;

import java.util.ArrayList;
import java.util.List;

public record ParseResult(
        List<ClassNode> classes,
        List<MethodNode> methods,
        List<ModuleNode> modules,
        List<RelationEdge> relations
) {
    public static ParseResult empty() {
        return new ParseResult(List.of(), List.of(), List.of(), List.of());
    }

    public ParseResult merge(ParseResult other) {
        List<ClassNode> mergedClasses = new ArrayList<>(this.classes);
        mergedClasses.addAll(other.classes);
        List<MethodNode> mergedMethods = new ArrayList<>(this.methods);
        mergedMethods.addAll(other.methods);
        List<ModuleNode> mergedModules = new ArrayList<>(this.modules);
        mergedModules.addAll(other.modules);
        List<RelationEdge> mergedRelations = new ArrayList<>(this.relations);
        mergedRelations.addAll(other.relations);
        return new ParseResult(mergedClasses, mergedMethods, mergedModules, mergedRelations);
    }
}
