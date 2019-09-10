package io.quarkus.vertx.keycloak;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

@ApplicationScoped
public class VertxOAuth2IdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private volatile OAuth2Auth auth;

    public OAuth2Auth getAuth() {
        return auth;
    }

    public VertxOAuth2IdentityProvider setAuth(OAuth2Auth auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        CompletableFuture<SecurityIdentity> result = new CompletableFuture<>();
        auth.decodeToken(request.getToken().getToken(), new Handler<AsyncResult<AccessToken>>() {
            @Override
            public void handle(AsyncResult<AccessToken> event) {
                if (event.failed()) {
                    result.completeExceptionally(event.cause());
                    return;
                }
                AccessToken token = event.result();
                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                builder.setPrincipal(new QuarkusPrincipal(token.principal().getString("username")));
                JsonObject realmAccess = token.accessToken().getJsonObject("realm_access");
                if (realmAccess != null) {
                    JsonArray roles = realmAccess.getJsonArray("roles");
                    if (roles != null) {
                        for (Object authority : roles) {
                            builder.addRole(authority.toString());
                        }
                    }
                }
                builder.addCredential(request.getToken());
                result.complete(builder.build());
            }
        });

        return result;
    }
}
