/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapClassLoaderFactory {

    private static final String DOT_QUARKUS = ".quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String DEPLOYMENT_CP = "cp.deployment";

    public static final String PROP_CP_CACHE = "quarkus-cp-cache";
    public static final String PROP_PROJECT_DISCOVERY = "quarkus-project-discovery";
    public static final String PROP_OFFLINE = "quarkus-bootstrap-offline";

    public static BootstrapClassLoaderFactory newInstance() {
        return new BootstrapClassLoaderFactory();
    }

    public static URLClassLoader newClassLoader(ClassLoader parent, List<AppDependency> deps, List<Path> extraPaths) {
        final URL[] urls = new URL[deps.size() + extraPaths.size()];
        try {
            int i = 0;
            while (i < deps.size()) {
                urls[i] = deps.get(i).getArtifact().getPath().toUri().toURL();
                ++i;
            }
            for(Path p : extraPaths) {
                if(p == null) {
                    continue;
                }
                urls[i++] = p.toUri().toURL();
            }
            return new URLClassLoader(i != urls.length ? Arrays.copyOf(urls, i) : urls, parent);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a URL", e);
        }
    }

    private static Path resolveCachedCpPath(LocalProject project) {
        return Paths.get(PropertyUtils.getUserHome())
                .resolve(DOT_QUARKUS)
                .resolve(BOOTSTRAP)
                .resolve(project.getGroupId())
                .resolve(project.getArtifactId())
                .resolve(project.getVersion())
                .resolve(DEPLOYMENT_CP);
    }

    private ClassLoader parent;
    private Path appClasses;
    private List<Path> appCp = new ArrayList<>(1);
    private boolean localProjectsDiscovery;
    private boolean offline = true;
    private boolean enableClasspathCache;

    private BootstrapClassLoaderFactory() {
    }

    public BootstrapClassLoaderFactory setParent(ClassLoader parent) {
        this.parent = parent;
        return this;
    }

    public BootstrapClassLoaderFactory setAppClasses(Path appClasses) {
        this.appClasses = appClasses;
        addToClassPath(appClasses);
        return this;
    }

    public BootstrapClassLoaderFactory addToClassPath(Path path) {
        this.appCp.add(path);
        return this;
    }

    public BootstrapClassLoaderFactory setLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapClassLoaderFactory setOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public BootstrapClassLoaderFactory setClasspathCache(boolean enable) {
        this.enableClasspathCache = enable;
        return this;
    }

    public URLClassLoader newAllInclusiveClassLoader(boolean hierarchical) throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        try {
            final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder().setOffline(offline);
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                mvnBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }
            final AppModel appModel = new BootstrapAppModelResolver(mvnBuilder.build()).resolveModel(localProject.getAppArtifact());
            if (hierarchical) {
                final URLClassLoader cl = newClassLoader(parent, appModel.getUserDependencies(), appCp);
                try {
                    return newClassLoader(cl, appModel.getDeploymentDependencies(), Collections.emptyList());
                } catch (Throwable e) {
                    try {
                        cl.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    throw e;
                }
            }
            return newClassLoader(parent, appModel.getAllDependencies(), appCp);
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }

    public URLClassLoader newDeploymentClassLoader() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        final URLClassLoader ucl;
        Path cachedCpPath = null;
        long lastUpdated = 0;
        try {
            final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
                    ? LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses))
                    : LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            if (enableClasspathCache) {
                lastUpdated = localProject.getWorkspace().getLastModified();
                cachedCpPath = resolveCachedCpPath(localProject);
                if (Files.exists(cachedCpPath)) {
                    try (BufferedReader reader = Files.newBufferedReader(cachedCpPath)) {
                        String line = reader.readLine();
                        if (Long.valueOf(line) == lastUpdated) {
                            line = reader.readLine();
                            final List<URL> urls = new ArrayList<>();
                            while (line != null) {
                                urls.add(new URL(line));
                                line = reader.readLine();
                            }
                            System.out.println("re-created from cache");
                            return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
                        } else {
                            System.out.println("cache expired");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            final MavenArtifactResolver.Builder mvn = MavenArtifactResolver.builder()
                    .setOffline(offline)
                    .setWorkspace(localProject.getWorkspace());
            ucl = newClassLoader(parent, new BootstrapAppModelResolver(mvn.build()).resolveModel(localProject.getAppArtifact()).getDeploymentDependencies(), Collections.emptyList());
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
        if(cachedCpPath != null) {
            try {
                Files.createDirectories(cachedCpPath.getParent());
                try(BufferedWriter writer = Files.newBufferedWriter(cachedCpPath)) {
                    writer.write(Long.toString(lastUpdated));
                    writer.newLine();
                    for(URL url : ucl.getURLs()) {
                        writer.write(url.toExternalForm());
                        writer.newLine();
                    }
                }
                System.out.println("cached");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ucl;
    }
}
