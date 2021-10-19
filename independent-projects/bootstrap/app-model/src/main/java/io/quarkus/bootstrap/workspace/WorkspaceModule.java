package io.quarkus.bootstrap.workspace;

import io.quarkus.paths.PathCollection;
import java.io.File;

public interface WorkspaceModule {

    WorkspaceModuleId getId();

    File getModuleDir();

    File getBuildDir();

    CompilationUnit getMainCompilationUnit();

    CompilationUnit getTestCompilationUnit();

    PathCollection getBuildFiles();
}
