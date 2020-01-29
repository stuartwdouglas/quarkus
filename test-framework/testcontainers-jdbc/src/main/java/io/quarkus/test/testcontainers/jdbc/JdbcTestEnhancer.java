package io.quarkus.test.testcontainers.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.testcontainers.containers.JdbcDatabaseContainer;

import io.quarkus.test.common.QuarkusTestEnhancer;

public class JdbcTestEnhancer implements QuarkusTestEnhancer {

    protected static final String QUARKUS_DATASOURCE_URL = "quarkus.datasource.url";
    boolean set = false;
    JdbcDatabaseContainer container;

    @Override
    public void beforeStart(IndexView indexView) {
        Collection<AnnotationInstance> containers = indexView
                .getAnnotations(DotName.createSimple(QuarkusJdbcContainer.class.getName()));
        for (AnnotationInstance i : containers) {
            if (i.target().kind() == AnnotationTarget.Kind.FIELD) {
                if (Modifier.isStatic(i.target().asField().flags())) {
                    String field = i.target().asField().name();
                    try {
                        Class<?> clazz = Class.forName(i.target().asField().declaringClass().name().toString(), false,
                                getClass().getClassLoader());
                        Field f = clazz.getDeclaredField(field);
                        f.setAccessible(true);
                        if (JdbcDatabaseContainer.class.isAssignableFrom(f.getType())) {
                            container = (JdbcDatabaseContainer) f.get(null);
                            container.start();
                            String url = container.getJdbcUrl();
                            System.setProperty(QUARKUS_DATASOURCE_URL, url);
                            set = true;
                            return;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    @Override
    public void afterShutdown() {
        if (set) {
            container.stop();
            System.clearProperty(QUARKUS_DATASOURCE_URL);
        }
    }
}
