package io.quarkus.test.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodParametersInterceptor;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class WebTestHandler implements QuarkusTestMethodParametersInterceptor, QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(QuarkusTestMethodContext context) {

    }

    @Override
    public void interceptTestMethod(Method method, Object[] params) {
        WebTest webTest = method.getAnnotation(WebTest.class);
        if (webTest == null) {
            return;
        }
        ValidatableResponse response = RestAssured.request(webTest.method().toString(), webTest.value()).then();
        if (webTest.status() > 0) {
            response.statusCode(webTest.status());
        }
        if (method.getParameterTypes().length == 0) {
            //no parameters, we just assume that all the user wants to validate is the status code
            //maybe this should be an error
            return;
        }
        int paramIndex = 0;
        if (method.getParameterTypes().length > 1) {
            boolean done = false;
            for (int i = 0; i < method.getParameterAnnotations().length && !done; ++i) {
                for (Annotation ann : method.getParameterAnnotations()[i]) {
                    if (ann.annotationType() == Result.class) {
                        paramIndex = i;
                        done = true;
                        break;
                    }
                }
            }
            if (!done) {
                throw new RuntimeException(
                        "Could not determine result parameter for " + method + " please annotate a parameter with @Result");
            }
        }
        Class<?> type = method.getParameterTypes()[paramIndex];
        if (type == ValidatableResponse.class) {
            params[paramIndex] = response;
        } else if (type == String.class) {
            params[paramIndex] = response.extract().body().asString();
        } else {
            params[paramIndex] = response.extract().body().as(type);
        }

    }
}
