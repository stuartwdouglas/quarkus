package io.quarkus.launcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * Class that starts the integration process for JBang
 */
public class JBangRuntimeBuilder {

    public static Map<String, byte[]> postBuild(Path appClasses, Path pomFile, List<Map.Entry<String, Path>> dependencies) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            RuntimeLaunchClassLoader loader = new RuntimeLaunchClassLoader(
                    new ClassLoader(JBangRuntimeBuilder.class.getClassLoader()) {
                        @Override
                        public Class<?> loadClass(String name) throws ClassNotFoundException {
                            return loadClass(name, false);
                        }

                        @Override
                        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                            if (name.startsWith("org.")) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                throw new ClassNotFoundException();
                            }
                            return super.loadClass(name, resolve);
                        }

                        @Override
                        public URL getResource(String name) {
                            if (name.startsWith("org/")) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                return null;
                            }
                            return super.getResource(name);
                        }

                        @Override
                        public Enumeration<URL> getResources(String name) throws IOException {
                            if (name.startsWith("org/")) {
                                //jbang has some but not all of the maven resolver classes we need on its
                                //class path. These all start with org. so we filter them out to make sure
                                //we get a complete class path
                                return Collections.emptyEnumeration();
                            }
                            return super.getResources(name);
                        }
                    });
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> launcher = loader.loadClass("io.quarkus.bootstrap.JBangBuilderImpl");
            return (Map<String, byte[]>) launcher.getDeclaredMethod("postBuild", Path.class, Path.class, List.class).invoke(
                    null, appClasses, pomFile,
                    dependencies);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.clearProperty(BootstrapConstants.SERIALIZED_APP_MODEL);
            Thread.currentThread().setContextClassLoader(old);
        }
    }

}
