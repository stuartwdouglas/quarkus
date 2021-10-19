package io.quarkus.bootstrap.workspace;

import java.util.List;

public class DefaultCompilationUnit implements CompilationUnit {

    private final List<ProcessedSources> sources;
    private final List<ProcessedSources> resources;

    public DefaultCompilationUnit(List<ProcessedSources> sources, List<ProcessedSources> resources) {
        this.sources = sources;
        this.resources = resources;
    }

    @Override
    public List<ProcessedSources> getSources() {
        return sources;
    }

    @Override
    public List<ProcessedSources> getResources() {
        return resources;
    }
}
