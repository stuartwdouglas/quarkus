/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package io.quarkus.rest.test.validation;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.rest.test.validation.resource.ValidationThroughRestResource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Validator provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test - RESTEASY-1296
 * @tpSince RESTEasy 3.0.16
 */
public class ValidationThroughRestTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, ValidationThroughRestResource.class);
                }
            });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ValidationThroughRestTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Field and EJB parameter validation.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void validationOfFieldAndParameterOfEjbResource() {
        Client client = ClientBuilder.newClient();
        Builder builder = client.target(generateURL("/hikes/createHike")).request();
        builder.accept(MediaType.TEXT_PLAIN_TYPE);
        Response response = builder.post(Entity.entity("-1", MediaType.APPLICATION_JSON_TYPE));
        String responseBody = response.readEntity(String.class);
        Assert.assertThat("Wrong validation error", responseBody, containsString("must be greater than or equal to 1"));
        Assert.assertTrue("Wrong validation error",
                responseBody.contains("may not be null") || responseBody.contains("must not be null"));
        client.close();
    }
}
