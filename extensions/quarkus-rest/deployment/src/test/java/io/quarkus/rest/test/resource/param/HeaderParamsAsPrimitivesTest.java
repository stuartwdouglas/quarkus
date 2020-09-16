package io.quarkus.rest.test.resource.param;

import static io.quarkus.rest.test.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesArrayDefaultNullProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesArrayDefaultOverrideProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesArrayDefaultProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesArrayProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesDefaultNullProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesDefaultOverrideProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesDefaultProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesListDefaultNullProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesListDefaultOverrideProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesListDefaultProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesListProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesPrimitivesProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceArray;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceArrayDefault;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceArrayDefaultNull;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceArrayDefaultOverride;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceDefault;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceDefaultNull;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceDefaultOverride;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceList;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceListDefault;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceListDefaultNull;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceListDefaultOverride;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourcePrimitives;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceSet;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceSortedSet;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceWrappers;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceWrappersDefault;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceWrappersDefaultNull;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesResourceWrappersDefaultOverride;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesSetProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesSortedSetProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesWrappersDefaultNullProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesWrappersDefaultOverrideProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesWrappersDefaultProxy;
import io.quarkus.rest.test.resource.param.resource.HeaderParamsAsPrimitivesWrappersProxy;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test primitive header parameters
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Header Params As Primitives Test")
public class HeaderParamsAsPrimitivesTest {

    public static final String ERROR_MESSAGE = "Wrong content of header parameter";

    private static HeaderParamsAsPrimitivesPrimitivesProxy resourceHeaderPrimitives;

    private static HeaderParamsAsPrimitivesDefaultProxy resourceHeaderPrimitivesDefault;

    private static HeaderParamsAsPrimitivesDefaultOverrideProxy resourceHeaderPrimitivesDefaultOverride;

    private static HeaderParamsAsPrimitivesDefaultNullProxy resourceHeaderPrimitivesDefaultNull;

    private static HeaderParamsAsPrimitivesWrappersProxy resourceHeaderPrimitiveWrappers;

    private static HeaderParamsAsPrimitivesWrappersDefaultProxy resourceHeaderPrimitiveWrappersDefault;

    private static HeaderParamsAsPrimitivesWrappersDefaultOverrideProxy resourceHeaderPrimitiveWrappersDefaultOverride;

    private static HeaderParamsAsPrimitivesWrappersDefaultNullProxy resourceHeaderPrimitiveWrappersDefaultNull;

    private static HeaderParamsAsPrimitivesListProxy resourceHeaderPrimitiveList;

    private static HeaderParamsAsPrimitivesListDefaultProxy resourceHeaderPrimitiveListDefault;

    private static HeaderParamsAsPrimitivesListDefaultOverrideProxy resourceHeaderPrimitiveListDefaultOverride;

    private static HeaderParamsAsPrimitivesListDefaultNullProxy resourceHeaderPrimitiveListDefaultNull;

    private static HeaderParamsAsPrimitivesArrayProxy resourceHeaderPrimitiveArray;

    private static HeaderParamsAsPrimitivesArrayDefaultProxy resourceHeaderPrimitiveArrayDefault;

    private static HeaderParamsAsPrimitivesArrayDefaultOverrideProxy resourceHeaderPrimitiveArrayDefaultOverride;

    private static HeaderParamsAsPrimitivesArrayDefaultNullProxy resourceHeaderPrimitiveArrayDefaultNull;

    private QuarkusRestClient client;

    private static QuarkusRestClient proxyClient;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(HeaderParamsAsPrimitivesPrimitivesProxy.class, HeaderParamsAsPrimitivesDefaultProxy.class,
                    HeaderParamsAsPrimitivesDefaultOverrideProxy.class, HeaderParamsAsPrimitivesDefaultNullProxy.class,
                    HeaderParamsAsPrimitivesWrappersProxy.class, HeaderParamsAsPrimitivesWrappersDefaultProxy.class,
                    HeaderParamsAsPrimitivesWrappersDefaultNullProxy.class,
                    HeaderParamsAsPrimitivesWrappersDefaultOverrideProxy.class, HeaderParamsAsPrimitivesListProxy.class,
                    HeaderParamsAsPrimitivesSetProxy.class, HeaderParamsAsPrimitivesSortedSetProxy.class,
                    HeaderParamsAsPrimitivesListDefaultProxy.class, HeaderParamsAsPrimitivesListDefaultNullProxy.class,
                    HeaderParamsAsPrimitivesListDefaultOverrideProxy.class, HeaderParamsAsPrimitivesArrayProxy.class,
                    HeaderParamsAsPrimitivesArrayDefaultProxy.class, HeaderParamsAsPrimitivesArrayDefaultNullProxy.class,
                    HeaderParamsAsPrimitivesArrayDefaultOverrideProxy.class);
            return TestUtil.finishContainerPrepare(war, null, HeaderParamsAsPrimitivesResourcePrimitives.class,
                    HeaderParamsAsPrimitivesResourceDefault.class, HeaderParamsAsPrimitivesResourceDefaultOverride.class,
                    HeaderParamsAsPrimitivesResourceDefaultNull.class, HeaderParamsAsPrimitivesResourceWrappers.class,
                    HeaderParamsAsPrimitivesResourceWrappersDefault.class,
                    HeaderParamsAsPrimitivesResourceWrappersDefaultNull.class,
                    HeaderParamsAsPrimitivesResourceWrappersDefaultOverride.class, HeaderParamsAsPrimitivesResourceList.class,
                    HeaderParamsAsPrimitivesResourceSet.class, HeaderParamsAsPrimitivesResourceSortedSet.class,
                    HeaderParamsAsPrimitivesResourceListDefault.class, HeaderParamsAsPrimitivesResourceListDefaultNull.class,
                    HeaderParamsAsPrimitivesResourceListDefaultOverride.class, HeaderParamsAsPrimitivesResourceArray.class,
                    HeaderParamsAsPrimitivesResourceArrayDefault.class, HeaderParamsAsPrimitivesResourceArrayDefaultNull.class,
                    HeaderParamsAsPrimitivesResourceArrayDefaultOverride.class);
        }
    });

    private static String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(HeaderParamsAsPrimitivesTest.class.getSimpleName());
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, HeaderParamsAsPrimitivesTest.class.getSimpleName());
    }

    @BeforeAll
    public static void before() throws Exception {
        proxyClient = (QuarkusRestClient) ClientBuilder.newClient();
        resourceHeaderPrimitives = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesPrimitivesProxy.class).build();
        resourceHeaderPrimitivesDefault = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesDefaultProxy.class).build();
        resourceHeaderPrimitivesDefaultOverride = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesDefaultOverrideProxy.class).build();
        resourceHeaderPrimitivesDefaultNull = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesDefaultNullProxy.class).build();
        resourceHeaderPrimitiveWrappers = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesWrappersProxy.class).build();
        resourceHeaderPrimitiveWrappersDefault = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesWrappersDefaultProxy.class).build();
        resourceHeaderPrimitiveWrappersDefaultOverride = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesWrappersDefaultOverrideProxy.class).build();
        resourceHeaderPrimitiveWrappersDefaultNull = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesWrappersDefaultNullProxy.class).build();
        resourceHeaderPrimitiveList = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesListProxy.class).build();
        resourceHeaderPrimitiveListDefault = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesListDefaultProxy.class).build();
        resourceHeaderPrimitiveListDefaultOverride = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesListDefaultOverrideProxy.class).build();
        resourceHeaderPrimitiveListDefaultNull = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesListDefaultNullProxy.class).build();
        resourceHeaderPrimitiveArray = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesArrayProxy.class).build();
        resourceHeaderPrimitiveArrayDefault = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesArrayDefaultProxy.class).build();
        resourceHeaderPrimitiveArrayDefaultOverride = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesArrayDefaultOverrideProxy.class).build();
        resourceHeaderPrimitiveArrayDefaultNull = proxyClient.target(generateBaseUrl())
                .proxyBuilder(HeaderParamsAsPrimitivesArrayDefaultNullProxy.class).build();
    }

    @AfterAll
    public static void after() throws Exception {
        proxyClient.close();
    }

    public void basicTest(String type, String value) {
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL("/")).request().header(HttpHeaderNames.ACCEPT, "application/" + type)
                    .header(type, value).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL("/wrappers")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/" + type).header(type, value).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL("/list")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/" + type).header(type, value).header(type, value)
                    .header(type, value).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
    }

    public void testDefault(String base, String type, String value) {
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL(base + "default/null")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/" + type).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL(base + "default")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/" + type).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL(base + "default/override")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/" + type).header(type, value).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }
    }

    public void testDefault(String type, String value) {
        testDefault("/", type, value);
    }

    public void testWrappersDefault(String type, String value) {
        testDefault("/wrappers/", type, value);
    }

    public void testListDefault(String type, String value) {
        testDefault("/list/", type, value);
    }

    /**
     * @tpTestDetails Test set of boolean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Set")
    public void testSet() {
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL("/set")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/boolean").header("header", "one").header("header", "one")
                    .header("header", "one").header("header", "two").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
            client = (QuarkusRestClient) ClientBuilder.newClient();
            HeaderParamsAsPrimitivesSetProxy setClient = client.target(generateBaseUrl())
                    .proxyBuilder(HeaderParamsAsPrimitivesSetProxy.class).build();
            HashSet<String> set = new HashSet<>();
            set.add("one");
            set.add("two");
            setClient.doGetBoolean(set);
            client.close();
        }
        {
            client = (QuarkusRestClient) ClientBuilder.newClient();
            Response response = client.target(generateURL("/sortedset")).request()
                    .header(HttpHeaderNames.ACCEPT, "application/boolean").header("header", "one").header("header", "one")
                    .header("header", "one").header("header", "two").get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            response.close();
            client.close();
            client = (QuarkusRestClient) ClientBuilder.newClient();
            HeaderParamsAsPrimitivesSortedSetProxy setClient = client.target(generateBaseUrl())
                    .proxyBuilder(HeaderParamsAsPrimitivesSortedSetProxy.class).build();
            TreeSet<String> set = new TreeSet<String>();
            set.add("one");
            set.add("two");
            setClient.doGetBoolean(set);
            client.close();
        }
    }

    /**
     * @tpTestDetails Test list of boolean with GET method
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Boolean")
    public void testGetBoolean() {
        basicTest("boolean", "true");
        resourceHeaderPrimitives.doGet(true);
        resourceHeaderPrimitiveWrappers.doGet(Boolean.TRUE);
        ArrayList<Boolean> list = new ArrayList<>();
        list.add(Boolean.TRUE);
        list.add(Boolean.TRUE);
        list.add(Boolean.TRUE);
        resourceHeaderPrimitiveList.doGetBoolean(list);
        boolean[] array = { true, true, true };
        resourceHeaderPrimitiveArray.doGetBoolean(array);
    }

    /**
     * @tpTestDetails Basic test for boolean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Boolean Primitives Default")
    public void testGetBooleanPrimitivesDefault() {
        testDefault("boolean", "true");
        resourceHeaderPrimitivesDefault.doGetBoolean();
        resourceHeaderPrimitivesDefaultNull.doGetBoolean();
        resourceHeaderPrimitivesDefaultOverride.doGet(true);
    }

    /**
     * @tpTestDetails Boolean test by proxy
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Boolean Primitive Wrapper Default")
    public void testGetBooleanPrimitiveWrapperDefault() {
        testWrappersDefault("boolean", "true");
        resourceHeaderPrimitiveWrappersDefault.doGetBoolean();
        resourceHeaderPrimitiveWrappersDefaultNull.doGetBoolean();
        resourceHeaderPrimitiveWrappersDefaultOverride.doGet(Boolean.TRUE);
    }

    /**
     * @tpTestDetails Proxy test for list of boolean
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Boolean Primitive List Default")
    public void testGetBooleanPrimitiveListDefault() {
        testListDefault("boolean", "true");
        resourceHeaderPrimitiveListDefault.doGetBoolean();
        resourceHeaderPrimitiveListDefaultNull.doGetBoolean();
        List<Boolean> list = new ArrayList<>();
        list.add(Boolean.TRUE);
        resourceHeaderPrimitiveListDefaultOverride.doGetBoolean(list);
        resourceHeaderPrimitiveArrayDefault.doGetBoolean();
        resourceHeaderPrimitiveArrayDefaultNull.doGetBoolean();
        boolean[] array = { true };
        resourceHeaderPrimitiveArrayDefaultOverride.doGetBoolean(array);
    }

    /**
     * @tpTestDetails Basic test for byte
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Byte")
    public void testGetByte() {
        basicTest("byte", "127");
        try {
            resourceHeaderPrimitives.doGet((byte) 127);
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            fail("resourceHeaderPrimitives.doGet() failed:\n" + errors.toString());
        }
        resourceHeaderPrimitiveWrappers.doGet(new Byte((byte) 127));
        ArrayList<Byte> list = new ArrayList<Byte>();
        list.add(new Byte((byte) 127));
        list.add(new Byte((byte) 127));
        list.add(new Byte((byte) 127));
        try {
            resourceHeaderPrimitiveList.doGetByte(list);
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            fail("resourceHeaderPrimitiveList.doGetByte() failed:\n" + errors.toString());
        }
    }

    /**
     * @tpTestDetails Proxy test for byte
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Byte Primitives Default")
    public void testGetBytePrimitivesDefault() {
        testDefault("byte", "127");
        resourceHeaderPrimitivesDefault.doGetByte();
        resourceHeaderPrimitivesDefaultNull.doGetByte();
        resourceHeaderPrimitivesDefaultOverride.doGet((byte) 127);
    }

    /**
     * @tpTestDetails Proxy test for byte with wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Byte Primitive Wrappers Default")
    public void testGetBytePrimitiveWrappersDefault() {
        testWrappersDefault("byte", "127");
        resourceHeaderPrimitiveWrappersDefault.doGetByte();
        resourceHeaderPrimitiveWrappersDefaultNull.doGetByte();
        resourceHeaderPrimitiveWrappersDefaultOverride.doGet(new Byte((byte) 127));
    }

    /**
     * @tpTestDetails Basic test for byte list
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Byte Primitive List Default")
    public void testGetBytePrimitiveListDefault() {
        testListDefault("byte", "127");
        resourceHeaderPrimitiveListDefault.doGetByte();
        resourceHeaderPrimitiveListDefaultNull.doGetByte();
        List<Byte> list = new ArrayList<Byte>();
        list.add(new Byte((byte) 127));
        resourceHeaderPrimitiveListDefaultOverride.doGetByte(list);
    }

    /**
     * @tpTestDetails Basic test for short, use proxy
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Short")
    public void testGetShort() {
        basicTest("short", "32767");
        resourceHeaderPrimitives.doGet((short) 32767);
        resourceHeaderPrimitiveWrappers.doGet(new Short((short) 32767));
        ArrayList<Short> list = new ArrayList<Short>();
        list.add(new Short((short) 32767));
        list.add(new Short((short) 32767));
        list.add(new Short((short) 32767));
        resourceHeaderPrimitiveList.doGetShort(list);
    }

    /**
     * @tpTestDetails Basic test for short, test default value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Short Primtives Default")
    public void testGetShortPrimtivesDefault() {
        testDefault("short", "32767");
    }

    /**
     * @tpTestDetails Short type test, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Short Primtive Wrappers Default")
    public void testGetShortPrimtiveWrappersDefault() {
        testWrappersDefault("short", "32767");
    }

    /**
     * @tpTestDetails Short test, test default value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Short Primtive List Default")
    public void testGetShortPrimtiveListDefault() {
        testListDefault("short", "32767");
    }

    /**
     * @tpTestDetails Basic test for int
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Int")
    public void testGetInt() {
        basicTest("int", "2147483647");
    }

    /**
     * @tpTestDetails Check default value for int
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Int Primitives Default")
    public void testGetIntPrimitivesDefault() {
        testDefault("int", "2147483647");
    }

    /**
     * @tpTestDetails Test int with wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Int Primitive Wrappers Default")
    public void testGetIntPrimitiveWrappersDefault() {
        testWrappersDefault("int", "2147483647");
    }

    /**
     * @tpTestDetails Test list of int
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Int Primitive List Default")
    public void testGetIntPrimitiveListDefault() {
        testListDefault("int", "2147483647");
    }

    /**
     * @tpTestDetails Basic test for long
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Long")
    public void testGetLong() {
        basicTest("long", "9223372036854775807");
    }

    /**
     * @tpTestDetails Test default value for long
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Long Primitives Default")
    public void testGetLongPrimitivesDefault() {
        testDefault("long", "9223372036854775807");
    }

    /**
     * @tpTestDetails Test default value for long, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Long Primitive Wrappers Default")
    public void testGetLongPrimitiveWrappersDefault() {
        testWrappersDefault("long", "9223372036854775807");
    }

    /**
     * @tpTestDetails Test default value for list of long, do not use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Long Primitive List Default")
    public void testGetLongPrimitiveListDefault() {
        testListDefault("long", "9223372036854775807");
    }

    /**
     * @tpTestDetails Basic test for float
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Float")
    public void testGetFloat() {
        basicTest("float", "3.14159265");
    }

    /**
     * @tpTestDetails Test default value for float
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Float Primitives Default")
    public void testGetFloatPrimitivesDefault() {
        testDefault("float", "3.14159265");
    }

    /**
     * @tpTestDetails Test default value for float, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Float Primitive Wrappers Default")
    public void testGetFloatPrimitiveWrappersDefault() {
        testWrappersDefault("float", "3.14159265");
    }

    /**
     * @tpTestDetails Test default value for list of float, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Float Primitive List Default")
    public void testGetFloatPrimitiveListDefault() {
        testListDefault("float", "3.14159265");
    }

    /**
     * @tpTestDetails Basic test for double
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Double")
    public void testGetDouble() {
        basicTest("double", "3.14159265358979");
    }

    /**
     * @tpTestDetails Basic test for double, test default value
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Double Primitives Default")
    public void testGetDoublePrimitivesDefault() {
        testDefault("double", "3.14159265358979");
    }

    /**
     * @tpTestDetails Basic test for double, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Double Primitive Wrappers Default")
    public void testGetDoublePrimitiveWrappersDefault() {
        testWrappersDefault("double", "3.14159265358979");
    }

    /**
     * @tpTestDetails Basic test for list of double, do not use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Double Primitive List Default")
    public void testGetDoublePrimitiveListDefault() {
        testListDefault("double", "3.14159265358979");
    }

    /**
     * @tpTestDetails Basic test for char
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Test Get Char")
    public void testGetChar() {
        basicTest("char", "a");
    }

    /**
     * @tpTestDetails Basic test for char, test default value
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Test Get Char Primitives Default")
    public void testGetCharPrimitivesDefault() {
        testDefault("char", "a");
    }

    /**
     * @tpTestDetails Basic test for char, use wrapper
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Test Get Char Primitive Wrappers Default")
    public void testGetCharPrimitiveWrappersDefault() {
        testWrappersDefault("char", "a");
    }

    /**
     * @tpTestDetails Basic test for list of char, do not use wrapper
     * @tpSince RESTEasy 3.0.24
     */
    @Test
    @DisplayName("Test Get Char Primitive List Default")
    public void testGetCharPrimitiveListDefault() {
        testListDefault("char", "a");
    }

    /**
     * @tpTestDetails Negative test for int
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad Primitive Value")
    public void testBadPrimitiveValue() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(generateURL("/")).request().header(HttpHeaderNames.ACCEPT, "application/int")
                .header("int", "abcdef").get();
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Negative test for int, use wrapper
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad Primitive Wrapper Value")
    public void testBadPrimitiveWrapperValue() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(generateURL("/wrappers")).request().header(HttpHeaderNames.ACCEPT, "application/int")
                .header("int", "abcdef").get();
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }

    /**
     * @tpTestDetails Negative test for list of int
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Bad Primitive List Value")
    public void testBadPrimitiveListValue() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        Response response = client.target(generateURL("/list")).request().header(HttpHeaderNames.ACCEPT, "application/int")
                .header("int", "abcdef").header("int", "abcdef").header("int", "abcdef").get();
        Assertions.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        response.close();
        client.close();
    }
}
