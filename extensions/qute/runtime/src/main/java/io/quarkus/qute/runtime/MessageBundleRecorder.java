package io.quarkus.qute.runtime;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MessageBundleRecorder {

    public Supplier<Object> createContext(Map<String, String> messageTemplates,
            Map<String, Map<String, Class<?>>> bundleInterfaces) {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                return new BundleContext() {

                    @Override
                    public Map<String, String> getMessageTemplates() {
                        return messageTemplates;
                    }

                    @Override
                    public Map<String, Map<String, Class<?>>> getBundleInterfaces() {
                        return bundleInterfaces;
                    }
                };
            }
        };
    }

    public Supplier<Object> defaultConstructorInvokerSupplier(String className) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(className).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public interface BundleContext {

        // message id -> message template
        Map<String, String> getMessageTemplates();

        // bundle -> (locale -> interface class)
        Map<String, Map<String, Class<?>>> getBundleInterfaces();

    }
}
