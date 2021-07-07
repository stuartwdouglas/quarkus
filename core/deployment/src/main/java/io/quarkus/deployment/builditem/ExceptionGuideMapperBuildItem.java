package io.quarkus.deployment.builditem;

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;

public final class ExceptionGuideMapperBuildItem extends MultiBuildItem implements Comparable<ExceptionGuideMapperBuildItem> {

    final int priority;
    final Function<Throwable, String> mapper;

    public ExceptionGuideMapperBuildItem(int priority, Function<Throwable, String> mapper) {
        this.priority = priority;
        this.mapper = mapper;
    }

    public int getPriority() {
        return priority;
    }

    public Function<Throwable, String> getMapper() {
        return mapper;
    }

    @Override
    public int compareTo(ExceptionGuideMapperBuildItem o) {
        return Integer.compare(priority, o.priority);
    }
}
