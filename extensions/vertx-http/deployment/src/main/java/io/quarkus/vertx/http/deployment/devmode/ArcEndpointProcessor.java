package io.quarkus.vertx.http.deployment.devmode;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.devmode.ArcEndpointRecorder;

public class ArcEndpointProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerRoutes(ArcConfig arcConfig, ArcEndpointRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        String basePath = "arc";
        String beansPath = basePath + "/beans";
        String removedBeansPath = basePath + "/removed-beans";
        String observersPath = basePath + "/observers";
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(basePath)
                .handler(recorder.createSummaryHandler(getConfigProperties(arcConfig))).build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(beansPath)
                .handler(recorder.createBeansHandler()).build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(removedBeansPath)
                .handler(recorder.createRemovedBeansHandler()).build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(observersPath)
                .handler(recorder.createObserversHandler()).build());
        // Use NonApplicationRootPathBuildItem to resolve path
        displayableEndpoints
                .produce(new NotFoundPageDisplayableEndpointBuildItem(nonApplicationRootPathBuildItem.resolvePath(basePath)));
        displayableEndpoints
                .produce(new NotFoundPageDisplayableEndpointBuildItem(nonApplicationRootPathBuildItem.resolvePath(beansPath)));
        displayableEndpoints.produce(
                new NotFoundPageDisplayableEndpointBuildItem(nonApplicationRootPathBuildItem.resolvePath(removedBeansPath)));
        displayableEndpoints.produce(
                new NotFoundPageDisplayableEndpointBuildItem(nonApplicationRootPathBuildItem.resolvePath(observersPath)));
    }

    // Note that we can't turn ArcConfig into BUILD_AND_RUN_TIME_FIXED because it's referencing IndexDependencyConfig
    // And we can't split the config due to compatibility reasons 
    private Map<String, String> getConfigProperties(ArcConfig arcConfig) {
        Map<String, String> props = new HashMap<>();
        props.put("quarkus.arc.remove-unused-beans", arcConfig.removeUnusedBeans);
        props.put("quarkus.arc.unremovable-types", arcConfig.unremovableTypes.map(Object::toString).orElse(""));
        props.put("quarkus.arc.detect-unused-false-positives", "" + arcConfig.detectUnusedFalsePositives);
        props.put("quarkus.arc.transform-unproxyable-classes", "" + arcConfig.transformUnproxyableClasses);
        props.put("quarkus.arc.auto-inject-fields", "" + arcConfig.autoInjectFields);
        props.put("quarkus.arc.auto-producer-methods", "" + arcConfig.autoProducerMethods);
        props.put("quarkus.arc.selected-alternatives", "" + arcConfig.selectedAlternatives.map(Object::toString).orElse(""));
        props.put("quarkus.arc.exclude-types", "" + arcConfig.excludeTypes.map(Object::toString).orElse(""));
        props.put("quarkus.arc.config-properties-default-naming-strategy",
                "" + arcConfig.configPropertiesDefaultNamingStrategy.toString());
        return props;
    }

}
