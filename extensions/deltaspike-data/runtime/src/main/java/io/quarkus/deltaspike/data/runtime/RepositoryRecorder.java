package io.quarkus.deltaspike.data.runtime;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import org.apache.deltaspike.data.impl.RepositoryExtension;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RepositoryRecorder {

    public Supplier<RepositoryExtension> repoExtension(List<String> repositories) {

        try {
            RepositoryExtension extension = new RepositoryExtension();
            Field repositoryClasses = extension.getClass().getDeclaredField("REPOSITORY_CLASSES");
            repositoryClasses.setAccessible(true);
            List<Class<?>> repos = (List<Class<?>>) repositoryClasses
                    .get(null);
            for (String i : repositories) {
                repos.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Supplier<RepositoryExtension>() {
            @Override
            public RepositoryExtension get() {
                try {
                    RepositoryExtension extension = new RepositoryExtension();
                    Field repositoryClasses = extension.getClass().getDeclaredField("repositoryClasses");
                    repositoryClasses.setAccessible(true);
                    List<Class<?>> repos = (List<Class<?>>) repositoryClasses
                            .get(extension);
                    for (String i : repositories) {
                        repos.add(Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
                    }
                    return extension;

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
