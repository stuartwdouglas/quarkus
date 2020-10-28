package io.quarkus.rest.common.runtime.model;

public class BeanParamInfo implements InjectableBean {
    private boolean isFormParamRequired;
    private boolean isInjectionRequired;

    @Override
    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    @Override
    public InjectableBean setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }

    @Override
    public boolean isInjectionRequired() {
        return isInjectionRequired;
    }

    @Override
    public InjectableBean setInjectionRequired(boolean isInjectionRequired) {
        this.isInjectionRequired = isInjectionRequired;
        return this;
    }
}
