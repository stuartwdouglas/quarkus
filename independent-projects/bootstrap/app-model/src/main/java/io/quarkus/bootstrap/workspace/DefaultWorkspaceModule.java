package io.quarkus.bootstrap.workspace;

import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class DefaultWorkspaceModule implements WorkspaceModule, Serializable {

    private final WorkspaceModuleId id;
    private final File moduleDir;
    private final File buildDir;
    private final List<ProcessedSources> mainSources = new ArrayList<>(1);
    private final List<ProcessedSources> mainResources = new ArrayList<>(1);
    private final List<ProcessedSources> testSources = new ArrayList<>(1);
    private final List<ProcessedSources> testResources = new ArrayList<>(1);
    private final CompilationUnit main;
    private final CompilationUnit test;
    private PathCollection buildFiles;

    public DefaultWorkspaceModule(WorkspaceModuleId id, File moduleDir, File buildDir) {
        super();
        this.id = id;
        this.moduleDir = moduleDir;
        this.buildDir = buildDir;
        this.main = new DefaultCompilationUnit(mainSources, mainResources);
        this.test = new DefaultCompilationUnit(testSources, testResources);
    }

    @Override
    public WorkspaceModuleId getId() {
        return id;
    }

    @Override
    public File getModuleDir() {
        return moduleDir;
    }

    @Override
    public File getBuildDir() {
        return buildDir;
    }

    public void addMainSources(ProcessedSources mainSources) {
        this.mainSources.add(mainSources);
    }

    public void addMainResources(ProcessedSources mainResources) {
        this.mainResources.add(mainResources);
    }

    @Override
    public CompilationUnit getMainCompilationUnit() {
        return null;
    }

    @Override
    public CompilationUnit getTestCompilationUnit() {
        return null;
    }

    public void addTestSources(ProcessedSources testSources) {
        this.testSources.add(testSources);
    }

    public void addTestResources(ProcessedSources testResources) {
        this.testResources.add(testResources);
    }

    public void setBuildFiles(PathCollection buildFiles) {
        this.buildFiles = buildFiles;
    }

    @Override
    public PathCollection getBuildFiles() {
        return buildFiles == null ? PathList.empty() : buildFiles;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(id);
        buf.append(" ").append(moduleDir);
        appendSources(buf, "sources", getMainCompilationUnit().getSources());
        appendSources(buf, "resources", getMainCompilationUnit().getResources());
        appendSources(buf, "test-sources", getTestCompilationUnit().getSources());
        appendSources(buf, "test-resources", getTestCompilationUnit().getResources());
        return buf.toString();
    }

    private void appendSources(StringBuilder buf, String name, Collection<ProcessedSources> sources) {
        if (!sources.isEmpty()) {
            buf.append(" ").append(name).append("[");
            final Iterator<ProcessedSources> i = sources.iterator();
            buf.append(i.next());
            while (i.hasNext()) {
                buf.append(";").append(i.next());
            }
            buf.append("]");
        }
    }
}
