package io.quarkus.elytron.security.jdbc;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * A configuration object for a jdbc based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm}
 */
@ConfigRoot(name = "security.jdbc", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JdbcSecurityRealmConfig {

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * If the JDBC store is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The principal-queries config
     */
    @ConfigItem(name = "principal-query")
    public PrincipalQueriesConfig principalQueries;
    //  https://github.com/wildfly/wildfly-core/blob/master/elytron/src/test/resources/org/wildfly/extension/elytron/security-realms.xml#L18

    @Override
    public String toString() {
        return "JdbcRealmConfig{" +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", principalQueries=" + principalQueries +
                '}';
    }
}
