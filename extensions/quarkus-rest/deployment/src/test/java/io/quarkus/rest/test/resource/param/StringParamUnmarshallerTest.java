package io.quarkus.rest.test.resource.param;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

import org.jboss.resteasy.annotations.StringParameterUnmarshallerBinder;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.param.resource.StringParamUnmarshallerDateFormatter;
import io.quarkus.rest.test.resource.param.resource.StringParamUnmarshallerFruit;
import io.quarkus.rest.test.resource.param.resource.StringParamUnmarshallerService;
import io.quarkus.rest.test.resource.param.resource.StringParamUnmarshallerSport;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resource
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 * @tpTestCaseDetails Test for unmarshalling with string parameter. StringParameterUnmarshallerBinder annotation is used
 */
public class StringParamUnmarshallerTest {
    @Retention(RetentionPolicy.RUNTIME)
    @StringParameterUnmarshallerBinder(StringParamUnmarshallerDateFormatter.class)
    public @interface StringParamUnmarshallerDateFormat {
        String value();
    }

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(StringParamUnmarshallerDateFormatter.class);
                    war.addClass(StringParamUnmarshallerFruit.class);
                    war.addClass(StringParamUnmarshallerSport.class);
                    war.addClass(StringParamUnmarshallerDateFormat.class);
                    return TestUtil.finishContainerPrepare(war, null, StringParamUnmarshallerService.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, StringParamUnmarshallerTest.class.getSimpleName());
    }

    @Test
    public void testDate() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(generateURL("/datetest/04-23-1977")).request();
        String date = request.get(String.class);
        Assert.assertTrue("Received wrong date", date.contains("Sat Apr 23 00:00:00"));
        Assert.assertTrue("Received wrong date", date.contains("1977"));
        client.close();
    }

    @Test
    public void testFruitAndSport() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        Invocation.Builder request = client.target(generateURL("/fromstring/ORANGE/football")).request();
        Assert.assertEquals("Received wrong response", "footballORANGE", request.get(String.class));
        client.close();
    }
}
