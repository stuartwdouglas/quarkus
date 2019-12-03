package io.quarkus.runner.bootstrap;

import java.nio.file.Path;

/**
 * This represents the runtime class path of the project, as defined by the build file.
 *
 * Unfortunately we cannot rely on the build tool to just feed this into Quarkus,
 * as we also need to support other cases such as running from the IDE.
 *
 * In practice this means we have four different ways of building this:
 *
 * - Maven passes in a list of runtime dependencies as resolved in the pom
 * - Gradle passes in a list of runtime dependencies as resolved from the build file
 * - We figure it out from the current class path in IDE execution (i.e. we guess)
 * - We load it from a pom in a pre-build jar file
 *
 * This information is used to resolve all the deployment artifacts and all other
 * Quarkus class loaders.
 *
 * Note that the resolved class path may include the 'application root'.
 *
 */
public interface QuarkusClassPathResolver {

    QuarkusClassPath resolveClassPath(Path applicationRoot);

}
