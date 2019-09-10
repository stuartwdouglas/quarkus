package io.quarkus.vertx.keycloak;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.rbac.impl.KeycloakRBACImpl;

@Recorder
public class VertxKeycloakRecorder {

    public void setup(KeycloakConfig config, RuntimeValue<Vertx> vertx, BeanContainer beanContainer) {
        OAuth2ClientOptions options = new OAuth2ClientOptions()
                .setSite(config.authServerUrl);

        if (config.resource.isPresent()) {
            options.setClientID(config.resource.get());
        }

        if (config.credentials.secret.isPresent()) {
            options.setClientSecret(config.credentials.secret.get());
        }

        if (!config.publicClient) {
            options.setUseBasicAuthorizationHeader(true);
        }

        final String realm = config.realm;

        options.setAuthorizationPath("/realms/" + realm + "/protocol/openid-connect/auth");
        options.setTokenPath("/realms/" + realm + "/protocol/openid-connect/token");
        options.setRevocationPath(null);
        options.setLogoutPath("/realms/" + realm + "/protocol/openid-connect/logout");
        options.setUserInfoPath("/realms/" + realm + "/protocol/openid-connect/userinfo");
        // keycloak follows the RFC7662
        options.setIntrospectionPath("/realms/" + realm + "/protocol/openid-connect/token/introspect");
        // keycloak follows the RFC7517
        options.setJwkPath("/realms/" + realm + "/protocol/openid-connect/certs");

        if (config.realmPublicKey.isPresent()) {
            options.addPubSecKey(new PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setPublicKey(config.realmPublicKey.get()));
        }

        OAuth2Auth auth = OAuth2Auth.create(vertx.getValue(), OAuth2FlowType.AUTH_CODE, options);
        auth.rbacHandler(new KeycloakRBACImpl(options));

        beanContainer.instance(VertxOAuth2IdentityProvider.class).setAuth(auth);
        VertxOAuth2AuthenticationMechanism mechanism = beanContainer.instance(VertxOAuth2AuthenticationMechanism.class);
        mechanism.setAuth(auth);
        mechanism.setAuthServerURI(config.authServerUrl);

    }
}
