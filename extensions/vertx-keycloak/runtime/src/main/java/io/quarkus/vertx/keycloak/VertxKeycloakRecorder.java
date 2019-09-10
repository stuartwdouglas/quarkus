package io.quarkus.vertx.keycloak;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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

        byte[] bogus = new byte[512];
        new SecureRandom().nextBytes(bogus);

        options.addPubSecKey(new PubSecKeyOptions().setSymmetric(true).setPublicKey(Base64.encode(bogus)).setAlgorithm("HS512"));

        options.setFlow(OAuth2FlowType.AUTH_JWT);

        OAuth2Auth auth = OAuth2Auth.create(vertx.getValue(), options);
        CompletableFuture<Void> cf = new CompletableFuture<>();
        auth.loadJWK(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                //TODO: handle this better
                if (event.failed()) {
                    cf.completeExceptionally(event.cause());
                } else {
                    cf.complete(null);
                }
            }
        });
        cf.join();
        KeycloakRBACImpl rbac = new KeycloakRBACImpl(options);
        auth.rbacHandler(rbac);

        beanContainer.instance(VertxOAuth2IdentityProvider.class).setAuth(auth);
        VertxOAuth2AuthenticationMechanism mechanism = beanContainer.instance(VertxOAuth2AuthenticationMechanism.class);
        mechanism.setAuth(auth);
        mechanism.setAuthServerURI(config.authServerUrl);

    }
}
