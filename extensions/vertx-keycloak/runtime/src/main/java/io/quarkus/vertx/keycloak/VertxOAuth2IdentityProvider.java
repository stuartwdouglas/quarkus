package io.quarkus.vertx.keycloak;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
                String scope = token.principal().getString("scope");
                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                builder.setPrincipal(new QuarkusPrincipal(token.principal().getString("username")));
                // avoid the case when scope is the literal "null" value.
                if (scope != null) {
                    for (String authority : scope.split(Pattern.quote(auth.getScopeSeparator()))) {
                        System.out.println(authority);
                        builder.addRole(authority);
                    }
                }
                token.isAuthorized("admin", new Handler<AsyncResult<Boolean>>() {
                    @Override
                    public void handle(AsyncResult<Boolean> event) {
                        System.out.println("Usert " + token.principal().getString("username") + " AUTH " + event.result());
                    }
                });
                builder.addCredential(request.getToken());
                System.out.println(token);
                result.complete(builder.build());
            }
        });

        return result;
    }
}
