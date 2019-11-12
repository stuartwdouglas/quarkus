package io.quarkus.runtime;

import java.lang.reflect.Method;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ApplicationMainClassRecorder {

    public void startApplication(Class<?> mainClass) throws Exception {
        Method m;
        try {
            m = mainClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find main method", e);
        }
        m.invoke(null, (Object) CommandLineArguments.getCommandLineArguments());
    }

}
