package io.quarkus.rest.common.deployment;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ApplicationResultBuildItem extends SimpleBuildItem {

    final Set<String> allowedClasses;
    final Set<String> singletonClasses;
    final Set<String> globalNameBindings;
    final boolean filterClasses;
    final Application application;
    final ClassInfo selectedAppClass;
    final boolean blocking;

    public ApplicationResultBuildItem(Set<String> allowedClasses, Set<String> singletonClasses, Set<String> globalNameBindings,
            boolean filterClasses, Application application, ClassInfo selectedAppClass, boolean blocking) {
        this.allowedClasses = allowedClasses;
        this.singletonClasses = singletonClasses;
        this.globalNameBindings = globalNameBindings;
        this.filterClasses = filterClasses;
        this.application = application;
        this.selectedAppClass = selectedAppClass;
        this.blocking = blocking;
    }

    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }

    public Set<String> getSingletonClasses() {
        return singletonClasses;
    }

    public Set<String> getGlobalNameBindings() {
        return globalNameBindings;
    }

    public boolean isFilterClasses() {
        return filterClasses;
    }

    public Application getApplication() {
        return application;
    }

    public ClassInfo getSelectedAppClass() {
        return selectedAppClass;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
