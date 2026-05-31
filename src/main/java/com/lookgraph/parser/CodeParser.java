package com.lookgraph.parser;

import com.lookgraph.common.enums.Language;

import java.nio.file.Path;

public interface CodeParser {

    boolean supports(Language language);

    ParseResult parseFile(Path file);

    ParseResult parseProject(Path projectRoot);
}
