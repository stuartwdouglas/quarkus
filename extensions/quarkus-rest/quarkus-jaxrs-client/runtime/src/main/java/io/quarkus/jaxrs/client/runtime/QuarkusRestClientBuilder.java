package io.quarkus.jaxrs.client.runtime;

import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import io.quarkus.rest.common.runtime.core.GenericTypeMapping;
import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;

public class QuarkusRestClientBuilder extends ClientBuilder {

    private ClientProxies clientProxies;
    private QuarkusRestConfiguration configuration;
    private SSLContext sslContext;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private char[] keystorePassword;
    private HostnameVerifier hostnameVerifier;

    @Override
    public ClientBuilder withConfig(Configuration config) {
        this.configuration = new QuarkusRestConfiguration(config);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        this.keyStore = null;
        this.trustStore = null;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        this.hostnameVerifier = verifier;
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public Client build() {
        Serialisers serialisers = JaxrsClientRecorder.getSerialisers();
        GenericTypeMapping genericTypeMapping = JaxrsClientRecorder.getGenericTypeMapping();
        if (serialisers == null) {
            serialisers = new ClientSerialisers();
            serialisers.registerBuiltins(RuntimeType.CLIENT);
        }
        if (genericTypeMapping == null) {
            genericTypeMapping = new GenericTypeMapping();
        }
        return new QuarkusRestClient(configuration, serialisers,
                JaxrsClientRecorder.getClientProxies(), genericTypeMapping, hostnameVerifier,
                sslContext, VertxCoreRecorder.getVertx());

    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        configuration.property(name, value);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass) {
        configuration.register(componentClass);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, int priority) {
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component) {
        configuration.register(component);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, int priority) {
        configuration.register(component, priority);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, Class<?>... contracts) {
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        configuration.register(component, contracts);
        return this;
    }
}
