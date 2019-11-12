package io.quarkus.deployment.pkg.steps;

import java.lang.reflect.Method;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.runtime.ApplicationMainClassRecorder;

public class ApplicationMainClassBuildStep {

    @BuildStep(loadsApplicationClasses = true)
    @Record(ExecutionTime.RUNTIME_INIT)
    ReflectiveMethodBuildItem startMain(ApplicationMainClassRecorder recorder,
            List<ServiceStartBuildItem> startBuildItem,
            PackageConfig config) throws Exception {
        if (!config.mainClass.isPresent()) {
            return null;
        }
        try {
            Class<?> mainClass = Class.forName(config.mainClass.get(), false, Thread.currentThread().getContextClassLoader());
            Method m = mainClass.getMethod("main", String[].class);
            recorder.startApplication(mainClass);
            return new ReflectiveMethodBuildItem(m);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find main method on " + config.mainClass.get(), e);
        }

    }
}
