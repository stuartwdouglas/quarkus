package io.quarkus.rest.test.form;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.form.resource.FormResource;
import io.quarkus.rest.test.form.resource.FormResourceClientForm;
import io.quarkus.rest.test.form.resource.FormResourceClientFormSecond;
import io.quarkus.rest.test.form.resource.FormResourceClientProxy;
import io.quarkus.rest.test.form.resource.FormResourceProxy;
import io.quarkus.rest.test.form.resource.FormResourceSecond;
import io.quarkus.rest.test.form.resource.FormResourceValueHolder;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Form tests
 * @tpChapter Integration tests
 * @tpTestCaseDetails Form test with resource
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Form Resource Test")
public class FormResourceTest {

    private static final String SHORT_VALUE_FIELD = "shortValue";

    private static final String INTEGER_VALUE_FIELD = "integerValue";

    private static final String LONG_VALUE_FIELD = "longValue";

    private static final String DOUBLE_VALUE_FIELD = "doubleValue";

    private static final String NAME_FIELD = "name";

    private static final String BOOLEAN_VALUE_FIELD = "booleanValue";

    private static final String TEST_URI = generateURL("/form/42?query=42");

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(FormResourceClientForm.class, FormResourceClientFormSecond.class, FormResourceClientProxy.class,
                    FormResourceProxy.class, FormResourceValueHolder.class);
            return TestUtil.finishContainerPrepare(war, null, FormResourceSecond.class, FormResource.class);
        }
    });

    private static String generateURL(String path) {
        return PortProviderUtil.generateURL(path, FormResourceTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-261
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Multi Value Param")
    public void testMultiValueParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL("/myform/server"));
        Response response = target.request().get();
        int status = response.getStatus();
        Assertions.assertEquals(200, status);
        boolean sv1 = false;
        boolean sv2 = false;
        MultivaluedMap<String, String> form = response
                .readEntity(new javax.ws.rs.core.GenericType<MultivaluedMap<String, String>>() {
                });
        Assertions.assertEquals(2, form.get("servername").size());
        for (String str : form.get("servername")) {
            if (str.equals("srv1")) {
                sv1 = true;
            } else if (str.equals("srv2")) {
                sv2 = true;
            }
        }
        Assertions.assertTrue(sv1);
        Assertions.assertTrue(sv2);
        client.close();
    }

    /**
     * @tpTestDetails Regression test for RESTEASY-691
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Proxy 691")
    public void testProxy691() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL(""));
        FormResourceProxy proxy = target.proxy(FormResourceProxy.class);
        proxy.post(null);
        client.close();
    }

    /**
     * @tpTestDetails Test for different value type of form by proxy.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Proxy")
    public void testProxy() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        QuarkusRestWebTarget target = client.target(generateURL(""));
        FormResourceClientProxy proxy = target.proxy(FormResourceClientProxy.class);
        FormResourceClientForm form = new FormResourceClientForm();
        form.setBooleanValue(true);
        form.setName("This is My Name");
        form.setDoubleValue(123.45);
        form.setLongValue(566780L);
        form.setIntegerValue(3);
        form.setShortValue((short) 12345);
        form.setHeaderParam(42);
        form.setQueryParam(42);
        form.setId(42);
        MultivaluedMap<String, String> rtn = proxy.post(form);
        Assertions.assertEquals(rtn.getFirst(BOOLEAN_VALUE_FIELD), "true");
        Assertions.assertEquals(rtn.getFirst(NAME_FIELD), "This is My Name");
        Assertions.assertEquals(rtn.getFirst(DOUBLE_VALUE_FIELD), "123.45");
        Assertions.assertEquals(rtn.getFirst(LONG_VALUE_FIELD), "566780");
        Assertions.assertEquals(rtn.getFirst(INTEGER_VALUE_FIELD), "3");
        Assertions.assertEquals(rtn.getFirst(SHORT_VALUE_FIELD), "12345");
        String str = proxy.postString(form);
        String[] params = str.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < params.length; i++) {
            int index = params[i].indexOf('=');
            String key = params[i].substring(0, index).trim();
            String value = params[i].substring(index + 1).trim().replace('+', ' ');
            map.put(key, value);
        }
        Assertions.assertEquals(map.get(BOOLEAN_VALUE_FIELD), "true");
        Assertions.assertEquals(map.get(NAME_FIELD), "This is My Name");
        Assertions.assertEquals(map.get(DOUBLE_VALUE_FIELD), "123.45");
        Assertions.assertEquals(map.get(LONG_VALUE_FIELD), "566780");
        Assertions.assertEquals(map.get(INTEGER_VALUE_FIELD), "3");
        Assertions.assertEquals(map.get(SHORT_VALUE_FIELD), "12345");
        client.close();
    }

    /**
     * @tpTestDetails Test for different value type of form directly
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Form Resource")
    public void testFormResource() throws Exception {
        InputStream in = null;
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            QuarkusRestWebTarget target = client.target(TEST_URI);
            Invocation.Builder request = target.request();
            request.header("custom-header", "42");
            Form form = new Form().param(BOOLEAN_VALUE_FIELD, "true").param(NAME_FIELD, "This is My Name")
                    .param(DOUBLE_VALUE_FIELD, "123.45").param(LONG_VALUE_FIELD, "566780").param(INTEGER_VALUE_FIELD, "3")
                    .param(SHORT_VALUE_FIELD, "12345");
            Response response = request.post(Entity.form(form));
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            String contentType = response.getHeaderString("content-type");
            Assertions.assertEquals(contentType, "application/x-www-form-urlencoded");
            InputStream responseStream = response.readEntity(InputStream.class);
            in = new BufferedInputStream(responseStream);
            String formData = TestUtil.readString(in);
            String[] keys = formData.split("&");
            Map<String, String> values = new HashMap<String, String>();
            for (String pair : keys) {
                int index = pair.indexOf('=');
                if (index < 0) {
                    values.put(URLDecoder.decode(pair, StandardCharsets.UTF_8.name()), null);
                } else if (index > 0) {
                    values.put(URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8.name()));
                }
            }
            Assertions.assertEquals(values.get(BOOLEAN_VALUE_FIELD), "true");
            Assertions.assertEquals(values.get(NAME_FIELD), "This is My Name");
            Assertions.assertEquals(values.get(DOUBLE_VALUE_FIELD), "123.45");
            Assertions.assertEquals(values.get(LONG_VALUE_FIELD), "566780");
            Assertions.assertEquals(values.get(INTEGER_VALUE_FIELD), "3");
        } finally {
            if (in != null) {
                in.close();
            }
            client.close();
        }
    }
}
