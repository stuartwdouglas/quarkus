package io.quarkus.rest.test.providers.jackson2.whitelist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jackson2.whitelist.model.AbstractVehicle;
import io.quarkus.rest.test.providers.jackson2.whitelist.model.TestPolymorphicType;
import io.quarkus.rest.test.providers.jackson2.whitelist.model.air.Aircraft;
import io.quarkus.rest.test.providers.jackson2.whitelist.model.land.Automobile;
import io.quarkus.rest.test.providers.jackson2.whitelist.model.land.Automobile2;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jackson2 provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.5.0
 */
public class WhiteListPolymorphicTypeValidatorManualOverrideTest {

    protected static final Logger logger = Logger
            .getLogger(WhiteListPolymorphicTypeValidatorManualOverrideTest.class.getName());

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(WhiteListPolymorphicTypeValidatorManualOverrideTest.class);
                    return TestUtil.finishContainerPrepare(war, null, JaxRsActivator.class, TestRESTService.class,
                            TestPolymorphicType.class, AbstractVehicle.class, Automobile.class, Automobile2.class,
                            Aircraft.class, JacksonConfig.class);
                }
            });

    @Before
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @After
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, WhiteListPolymorphicTypeValidatorManualOverrideTest.class.getSimpleName());
    }

    @Test
    public void testManualOverrideGood() throws Exception {
        String response = sendPost(new TestPolymorphicType(new Automobile2()));
        logger.info("response: " + response);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("Response code: " + Status.CREATED));
        Assert.assertTrue(response.contains("Created"));
    }

    @Test
    public void testAircraftFailure() throws Exception {
        String response = sendPost(new TestPolymorphicType(new Aircraft()));
        logger.info("response: " + response);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("Response code: " + Status.BAD_REQUEST));
        Assert.assertTrue(response.contains("Configured `PolymorphicTypeValidator`") && response.contains("denied resolution"));
    }

    @Test
    public void testAutomobileFailure() throws Exception {
        String response = sendPost(new TestPolymorphicType(new Automobile()));
        logger.info("response: " + response);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("Response code: " + Status.BAD_REQUEST));
        Assert.assertTrue(response.contains("Configured `PolymorphicTypeValidator`") && response.contains("denied resolution"));
    }

    private String createJSONString(TestPolymorphicType t) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(t);
    }

    private String sendPost(TestPolymorphicType t) throws Exception {

        logger.info("Creating JSON test data");
        String jsonData = createJSONString(t);

        logger.info("jsonData: " + jsonData);

        String urlString = generateURL("/test/post");
        logger.info("POST data to : " + urlString);
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        byte[] out = jsonData.getBytes(StandardCharsets.UTF_8);

        http.setFixedLengthStreamingMode(out.length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }

        InputStream is = null;
        if (http.getResponseCode() != 200) {
            is = http.getErrorStream();
        } else {
            /* error from server */
            is = http.getInputStream();
        }

        String result = is == null ? ""
                : new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
        String response = String.format("Response code: %s response message: %s  %s", http.getResponseCode(),
                http.getResponseMessage(), result);

        logger.info("Response: " + response);

        return response;
    }

}
