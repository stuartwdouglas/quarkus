package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that can be used to customize the built {@link ServerResourceMethod} instances before the
 * final {@link org.jboss.resteasy.reactive.server.core.DeploymentInfo} is created
 */
public final class ServerResourceMethodCustomizerBuildItem extends MultiBuildItem {

    private final ServerResourceMethodCustomizer customizer;

    public ServerResourceMethodCustomizerBuildItem(ServerResourceMethodCustomizer customizer) {
        this.customizer = customizer;
    }

    public ServerResourceMethodCustomizer getCustomizer() {
        return customizer;
    }

    /**
     * Implementations will be invoked for each {@link ServerResourceMethod} discovered by Quarkus with the purpose of
     * altering the ServerResourceMethod
     */
    public interface ServerResourceMethodCustomizer {

        void customize(Context context);

        /**
         * The priority of the customizer.
         * All customizers are sorted by priority - in keeping with how JAX-RS handles priorities, lower priority
         * customizers are executed first
         */
        default int priority() {
            return 0;
        }

        interface Context {

            /**
             * The server resource method for which customization is applied
             */
            ServerResourceMethod resourceMethod();

            /**
             * The resource class that contains the resource method for which customization is applied
             * Implementations of {@link ServerResourceMethodCustomizer} should NOT alter this class in any way
             */
            ResourceClass resourceClass();

            /**
             * The path under which RESTEasy Reactive is deployed in the Quarkus application
             */
            String deploymentPath();
        }
    }
}
