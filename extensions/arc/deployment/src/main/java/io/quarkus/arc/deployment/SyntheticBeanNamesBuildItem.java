package io.quarkus.arc.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that contains all the registered synthetic beans
 * Build steps that need to know about synthetic beans should use this build item
 * instead of pulling information out of {@link SyntheticBeanBuildItem} because the use
 * of the later can easily lead to build cycles.
 */
public final class SyntheticBeanNamesBuildItem extends SimpleBuildItem {

    private final List<Entry> entries;

    SyntheticBeanNamesBuildItem(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private final DotName classDotName;
        private final boolean isRuntimeInit;

        public Entry(DotName classDotName, boolean isRuntimeInit) {
            this.classDotName = classDotName;
            this.isRuntimeInit = isRuntimeInit;
        }

        public DotName getClassDotName() {
            return classDotName;
        }

        public boolean isRuntimeInit() {
            return isRuntimeInit;
        }
    }
}
