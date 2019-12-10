package io.quarkus.test;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.app.CuratedApplication;

public class QuarkusUnitTestRunner implements BiConsumer<CuratedApplication, Map<String, Object>> {
    @Override
    public void accept(CuratedApplication curatedApplication, Map<String, Object> stringObjectMap) {

    }
}
