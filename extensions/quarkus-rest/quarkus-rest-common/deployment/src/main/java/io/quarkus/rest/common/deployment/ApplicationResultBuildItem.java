package io.quarkus.rest.common.deployment;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames;

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

    public KeepProviderResult keepProvider(ClassInfo providerClass) {
        if (filterClasses) {
            // we don't care about provider annotations, they're manually registered (but for the server only)
            return allowedClasses.contains(providerClass.name().toString()) ? KeepProviderResult.SERVER_ONLY
                    : KeepProviderResult.DISCARD;
        }
        return providerClass.classAnnotation(QuarkusRestDotNames.PROVIDER) != null ? KeepProviderResult.NORMAL
                : KeepProviderResult.DISCARD;
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

    public enum KeepProviderResult {
        NORMAL,
        SERVER_ONLY,
        DISCARD
    }

}
