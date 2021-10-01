package io.quarkus.deployment.dev.devservices;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

class DevServicesConfigBuildStep {
    private static final Logger log = Logger.getLogger(DevServicesConfigBuildStep.class);

    static volatile Map<String, String> oldConfig;
    static final Map<String, ContainerInfo> runningContainerInfo = new ConcurrentHashMap<>();

    @BuildStep
    List<DevServicesConfigResultBuildItem> deprecated(List<DevServicesNativeConfigResultBuildItem> items) {
        return items.stream().map(s -> new DevServicesConfigResultBuildItem(s.getKey(), s.getValue()))
                .collect(Collectors.toList());
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    DevServicesLauncherConfigResultBuildItem setup(BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfig,
            List<DevServicesConfigResultBuildItem> devServicesConfigResultBuildItems,
            GlobalDevServicesConfig globalDevServicesConfig,
            LaunchModeBuildItem launchModeBuildItem,
            CuratedApplicationShutdownBuildItem curatedApplicationShutdownBuildItem) {

        curatedApplicationShutdownBuildItem.addCloseTask(new Runnable() {
            @Override
            public void run() {
                for (var e : runningContainerInfo.entrySet()) {
                    try {
                        e.getValue().container.stop();
                    } catch (Throwable t) {
                        log.errorf(t, "Failed to shutdown %s", e.getKey());
                    }
                }
                runningContainerInfo.clear();
            }
        }, true);
        Map<String, String> newProperties = new HashMap<>();
        Map<String, String> newInternalProperties = new HashMap<>();
        for (var i : devServicesConfigResultBuildItems) {
            if (i.getKey().startsWith(DevServicesConfigResultBuildItem.INTERNAL_PREFIX)) {
                newInternalProperties.put(i.getKey().substring(DevServicesConfigResultBuildItem.INTERNAL_PREFIX.length()),
                        i.getValue());
            } else {
                newProperties.put(i.getKey(), i.getValue());
            }
        }
        for (var e : newProperties.entrySet()) {
            if (!newInternalProperties.containsKey(e.getKey())) {
                newInternalProperties.put(e.getKey(), e.getValue());
            }
        }
        Config config = ConfigProvider.getConfig();
        newProperties.putAll(handleCustomDevServices(globalDevServicesConfig, launchModeBuildItem,
                newInternalProperties));

        //check if there are existing already started dev services
        //if there were no changes to the processors they don't produce config
        //so we merge existing config from previous runs
        //we also check the current config, as the dev service may have been disabled by explicit config
        if (oldConfig != null) {
            for (Map.Entry<String, String> entry : oldConfig.entrySet()) {
                if (!newProperties.containsKey(entry.getKey())
                        && config.getOptionalValue(entry.getKey(), String.class).isEmpty()) {
                    newProperties.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            curatedApplicationShutdownBuildItem.addCloseTask(new Runnable() {
                @Override
                public void run() {
                    oldConfig = null;
                }
            }, true);
        }
        for (Map.Entry<String, String> entry : newProperties.entrySet()) {
            runtimeConfig.produce(new RunTimeConfigurationDefaultBuildItem(entry.getKey(), entry.getValue()));
        }
        oldConfig = newProperties;
        return new DevServicesLauncherConfigResultBuildItem(Collections.unmodifiableMap(newProperties));
    }

    private Map<String, String> handleCustomDevServices(GlobalDevServicesConfig globalDevServicesConfig,
            LaunchModeBuildItem launchModeBuildItem,
            Map<String, String> newProperties) {
        Map<String, String> ret = new HashMap<>();
        if (launchModeBuildItem.getLaunchMode().isDevOrTest()) {
            //now start custom dev services
            //these are started here so they have access to the config from Quarkus provided dev services
            //e.g. so they can use the same DB or access Kafka
            TreeMap<String, CustomDevServicesConfig> customConfig = new TreeMap<>(globalDevServicesConfig.custom);
            Map<String, String> interpolations = new HashMap<>();
            for (var i : newProperties.entrySet()) {
                interpolations.put("[" + i.getKey() + "]", i.getValue());
            }

            for (var e : customConfig.entrySet()) {

                var existing = runningContainerInfo.get(e.getKey());
                CustomDevServicesConfig devServiceConfig = e.getValue();
                if (existing != null) {
                    boolean shutdownRequired = false;
                    if (!existing.config.equals(devServiceConfig)) {
                        shutdownRequired = true;
                    } else {
                        for (var existingInterpol : existing.interpolations.entrySet()) {
                            if (!Objects.equals(interpolations.get(existingInterpol.getKey()), existingInterpol.getValue())) {
                                shutdownRequired = true;
                                break;
                            }
                        }
                    }
                    if (!shutdownRequired) {
                        continue;
                    }
                    existing.container.stop();
                    runningContainerInfo.remove(e.getKey());
                }

                QuarkusGenericContainer container = new QuarkusGenericContainer(devServiceConfig.image,
                        devServiceConfig.useSharedNetwork, e.getKey());
                Map<String, String> usedInterpolations = new HashMap<>();
                for (var env : devServiceConfig.env.entrySet()) {
                    String val = env.getValue();
                    if (val.contains("[") && val.contains("]")) {
                        for (var i : interpolations.entrySet()) {
                            if (val.contains(i.getKey())) {
                                usedInterpolations.put(i.getKey(), i.getValue());
                                val = val.replace(i.getKey(), i.getValue());
                            }
                        }
                    }
                    container.addEnv(env.getKey(), val);
                }
                if (devServiceConfig.ports.isPresent()) {
                    for (var port : devServiceConfig.ports.get()) {
                        container.addExposedPort(port);
                    }
                }
                container.start();
                container.followOutput(new Consumer<OutputFrame>() {
                    @Override
                    public void accept(OutputFrame outputFrame) {
                        System.out.println(outputFrame.getUtf8String());
                    }
                });
                if (devServiceConfig.ports.isPresent()) {
                    boolean first = true;
                    for (var port : devServiceConfig.ports.get()) {
                        String url = devServiceConfig.urlPrefix.orElse("") + container.getHostName() + ":"
                                + container.getMappedPort(port) + devServiceConfig.urlSuffix.orElse("");
                        String key = "quarkus.devservices.custom." + e.getKey() + ".mapped-port." + port;
                        interpolations.put("[" + key + "]", Integer.toString(container.getMappedPort(port)));
                        ret.put(key, Integer.toString(container.getMappedPort(port)));

                        key = "quarkus.devservices.custom." + e.getKey() + ".mapped-url." + port;
                        interpolations.put("[" + key + "]", url);
                        ret.put(key, url);
                        if (first) {
                            first = false;
                            key = "quarkus.devservices.custom." + e.getKey() + ".url";
                            interpolations.put("[" + key + "]", url);
                            ret.put(key, url);
                        }
                    }
                }
                runningContainerInfo.put(e.getKey(), new ContainerInfo(devServiceConfig, container, usedInterpolations));
            }
        }
        return ret;
    }

    static class ContainerInfo {
        final CustomDevServicesConfig config;
        final QuarkusGenericContainer container;
        final Map<String, String> interpolations;

        ContainerInfo(CustomDevServicesConfig config, QuarkusGenericContainer container, Map<String, String> interpolations) {
            this.config = config;
            this.container = container;
            this.interpolations = interpolations;
        }
    }

    private static final class QuarkusGenericContainer extends GenericContainer<QuarkusGenericContainer> {

        private final boolean useSharedNetwork;

        private String hostName = null;
        private final String name;

        @SuppressWarnings("deprecation")
        private QuarkusGenericContainer(String dockerImageName, boolean useSharedNetwork, String name) {
            super(dockerImageName);
            this.useSharedNetwork = useSharedNetwork;
            this.name = name;
            setNetwork(Network.SHARED);
        }

        @Override
        protected void configure() {
            super.configure();
            if (useSharedNetwork) {
                hostName = ConfigureUtil.configureSharedNetwork(this, name);
                return;
            }
        }

        public String getHostName() {
            if (useSharedNetwork) {
                return hostName;
            } else {
                return getContainerIpAddress();
            }
        }
    }
}
