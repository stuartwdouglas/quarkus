package io.quarkus.bootstrap.workspace;

import io.quarkus.bootstrap.model.ApplicationModel;
import java.util.List;
import java.util.Set;

public interface Workspace {

    WorkspaceModule getApplicationModule();

    List<WorkspaceModule> getWorkspaceModules();

    ApplicationModel getApplicationModel(WorkspaceModule module, boolean test);

    Set<WorkspaceModule> getModuleDependencies(WorkspaceModule module);
}
