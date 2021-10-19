package io.quarkus.bootstrap.workspace;

import java.util.List;

public interface CompilationUnit {

    List<ProcessedSources> getSources();

    List<ProcessedSources> getResources();
}
