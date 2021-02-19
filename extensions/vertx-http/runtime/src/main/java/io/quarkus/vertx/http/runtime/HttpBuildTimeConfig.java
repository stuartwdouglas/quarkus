package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.core.http.ClientAuth;

@ConfigRoot(name = "http", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class HttpBuildTimeConfig {

    /**
     * The HTTP root path. All web content will be served relative to this root path.
     */
    @ConfigItem(defaultValue = "/")
    public String rootPath;

    public AuthConfig auth;

    /**
     * Configures the engine to require/request client authentication.
     * NONE, REQUEST, REQUIRED
     */
    @ConfigItem(name = "ssl.client-auth", defaultValue = "NONE")
    public ClientAuth tlsClientAuth;

    /**
     * If this is true then only a virtual channel will be set up for vertx web.
     * We have this switch for testing purposes.
     */
    @ConfigItem
    public boolean virtual;

    /**
     * The HTTP root path for non-application endpoints. Various extension-provided endpoints such as metrics, health,
     * and openapi are deployed under this path.
     * <p>
     * <dl>
     *     <dt>Relative path (Default, {@literal q})</dt>
     *     <dd>Non-application endpoints will be served from {@literal quarkus.http.root-path}{@literal /}{@literal quarkus.http.non-application-root-path}.</dd>
     *     <dt>Absolute path ({@literal /q}</dt>
     *     <dd>Non-application endpoints will be served from the specified path.</dd>
     *     <dt>Empty path</dt>
     *     <dd>Non-application endpoints will be served from {@literal quarkus.http.root-path}.</dd>
     * </dl>
     */
    @ConfigItem(defaultValue = "q")
    public String nonApplicationRootPath;

    /**
     * Provide redirect endpoints for non-application endpoints existing prior to Quarkus 1.11.
     * This will trigger HTTP 301 Redirects for the following:
     * <ul>
     *     <li>/graphql-ui</li>
     *     <li>/health</li>
     *     <li>/health-ui</li>
     *     <li>/metrics</li>
     *     <li>/openapi</li>
     *     <li>/swagger-ui</li>
     * </ul>
     * <p>
     * Default is {@literal true} for Quarkus 1.11.x to facilitate transition to name-spaced URIs using {quarkus.http.non-application-root-path}.
     * <p>
     * Quarkus 1.12 will change the default to {@literal false},
     * and the config item will be removed in Quarkus 2.0.
     */
    @ConfigItem(defaultValue = "true")
    public boolean redirectToNonApplicationRootPath;
}
