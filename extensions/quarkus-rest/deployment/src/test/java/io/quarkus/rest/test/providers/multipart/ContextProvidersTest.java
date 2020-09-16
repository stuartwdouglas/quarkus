package io.quarkus.rest.test.providers.multipart;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jboss.resteasy.annotations.providers.multipart.XopWithMultipartRelated;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersCustomer;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersCustomerForm;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersCustomerFormNewAnnotationOnField;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersCustomerFormNewAnnotationOnSetter;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersName;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersResource;
import io.quarkus.rest.test.providers.multipart.resource.ContextProvidersXop;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-1119.
 *                    Unable to find contextual data of type: javax.ws.rs.ext.Providers if ClientBuilder.newClient is used.
 * @tpSince RESTEasy 3.0.16
 */
@SuppressWarnings("deprecation")
@DisplayName("Context Providers Test")
public class ContextProvidersTest {

    protected final Logger logger = Logger.getLogger(ContextProvidersTest.class.getName());

    public static final Annotation PART_TYPE_APPLICATION_XML = new S1() {

        private static final long serialVersionUID = 1L;

        @Override
        public String value() {
            return "application/xml";
        }
    };

    public static final Annotation MULTIPART_FORM = new S2() {

        private static final long serialVersionUID = 1L;
    };

    public static final Annotation XOP_WITH_MULTIPART_RELATED = new S3() {

        private static final long serialVersionUID = 1L;
    };

    static final MediaType MULTIPART_MIXED = new MediaType("multipart", "mixed");

    static final MediaType MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");

    static final MediaType MULTIPART_RELATED = new MediaType("multipart", "related");

    static final javax.ws.rs.core.GenericType<List<ContextProvidersName>> LIST_NAME_TYPE = new javax.ws.rs.core.GenericType<List<ContextProvidersName>>() {
    };

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(ContextProvidersCustomer.class, ContextProvidersCustomerForm.class,
                    ContextProvidersCustomerFormNewAnnotationOnField.class,
                    ContextProvidersCustomerFormNewAnnotationOnSetter.class, ContextProvidersName.class,
                    ContextProvidersXop.class, PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, ContextProvidersResource.class);
        }
    });

    /**
     * @tpTestDetails Form data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Form Data")
    public void testGetFormData() throws Exception {
        doTestGetFormData();
    }

    public void doTestGetFormData() throws Exception {
        try {
            MultipartFormDataInput entity = get("/get/form", MultipartFormDataInput.class);
            // Get parts by name.
            ContextProvidersCustomer c = entity.getFormDataPart("bill", ContextProvidersCustomer.class, null);
            Assertions.assertEquals("Bill", c.getName(), "Wrong response");
            String s = entity.getFormDataPart("bob", String.class, null);
            Assertions.assertEquals("Bob", s, "Wrong response");
            // Iterate over list of parts.
            for (Map.Entry<String, List<InputPart>> formDataEntry : entity.getFormDataMap().entrySet()) {
                // logger.debug("key: " + formDataEntry.getKey());
                for (InputPart inputPart : formDataEntry.getValue()) {
                    if (MediaType.APPLICATION_XML_TYPE.equals(inputPart.getMediaType())) {
                        c = inputPart.getBody(ContextProvidersCustomer.class, null);
                        Assertions.assertEquals("Bill", c.getName(), "Wrong response");
                    } else {
                        s = inputPart.getBody(String.class, null);
                        Assertions.assertEquals("Bob", s, "Wrong response");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    /**
     * @tpTestDetails Mixed data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Mixed")
    public void testGetMixed() throws Exception {
        doTestGetMixed();
    }

    void doTestGetMixed() throws Exception {
        try {
            MultipartInput entity = get("/get/mixed", MultipartInput.class);
            // Iterate over list of parts.
            List<InputPart> parts = entity.getParts();
            for (Iterator<InputPart> it = parts.iterator(); it.hasNext();) {
                InputPart inputPart = it.next();
                if (MediaType.APPLICATION_XML_TYPE.equals(inputPart.getMediaType())) {
                    ContextProvidersCustomer c = inputPart.getBody(ContextProvidersCustomer.class, null);
                    Assertions.assertEquals("Bill", c.getName(), "Wrong response");
                } else {
                    String s = inputPart.getBody(String.class, null);
                    Assertions.assertEquals("Bob", s, "Wrong response");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    /**
     * @tpTestDetails List data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get List")
    public void testGetList() throws Exception {
        doTestGetList();
    }

    void doTestGetList() throws Exception {
        try {
            MultipartInput entity = get("/get/list", MultipartInput.class);
            // Iterate over list of parts.
            List<InputPart> parts = entity.getParts();
            Set<String> customers = new HashSet<String>();
            for (Iterator<InputPart> it = parts.iterator(); it.hasNext();) {
                InputPart inputPart = it.next();
                customers.add(inputPart.getBody(ContextProvidersCustomer.class, null).getName());
            }
            Assert.assertThat("Wrong count of customers from response", new Integer(customers.size()), is(2));
            Assert.assertThat("Received customers list do not contain all items", customers, hasItems("Bill"));
            Assert.assertThat("Received customers list do not contain all items", customers, hasItems("Bob"));
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    /**
     * @tpTestDetails Map data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Map")
    public void testGetMap() throws Exception {
        doTestGetMap();
    }

    public void doTestGetMap() throws Exception {
        try {
            MultipartFormDataInput entity = get("/get/map", MultipartFormDataInput.class);
            // Get parts by name.
            ContextProvidersCustomer c = entity.getFormDataPart("bill", ContextProvidersCustomer.class, null);
            Assertions.assertEquals("Bill", c.getName(), "Wrong response");
            c = entity.getFormDataPart("bob", ContextProvidersCustomer.class, null);
            Assertions.assertEquals("Bob", c.getName(), "Wrong response");
            // Iterate over map of parts.
            Set<String> customers = new HashSet<>();
            for (Map.Entry<String, List<InputPart>> formDataEntry : entity.getFormDataMap().entrySet()) {
                for (InputPart inputPart : formDataEntry.getValue()) {
                    customers.add(inputPart.getBody(ContextProvidersCustomer.class, null).getName());
                }
            }
            Assert.assertThat("Wrong count of customers from response", new Integer(customers.size()), is(2));
            Assert.assertThat("Received customers list do not contain all items", customers, hasItems("Bill"));
            Assert.assertThat("Received customers list do not contain all items", customers, hasItems("Bob"));
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    /**
     * @tpTestDetails Related data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Related")
    public void testGetRelated() throws Exception {
        doTestGetRelated();
    }

    void doTestGetRelated() throws Exception {
        try {
            MultipartRelatedInput entity = get("/get/related", MultipartRelatedInput.class);
            // Iterate over map of parts.
            Map<String, InputPart> map = entity.getRelatedMap();
            Set<String> keys = map.keySet();
            Assertions.assertEquals(2, keys.size());
            Assert.assertThat("Wrong count of keys from response", new Integer(keys.size()), is(2));
            Assertions.assertTrue(keys.contains("bill"));
            Assertions.assertTrue(keys.contains("bob"));
            Assert.assertThat("Missing key from response", keys, hasItems("bill"));
            Assert.assertThat("Missing key from response", keys, hasItems("bob"));
            Set<String> parts = new HashSet<>();
            for (InputPart inputPart : map.values()) {
                parts.add(inputPart.getBody(String.class, null));
            }
            Assert.assertThat("Received customers list do not contain all items", parts, hasItems("Bill"));
            Assert.assertThat("Received customers list do not contain all items", parts, hasItems("Bob"));
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    /**
     * @tpTestDetails Multipart form data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Multipart Form")
    public void testGetMultipartForm() throws Exception {
        doTestGetMultipartForm();
    }

    void doTestGetMultipartForm() throws Exception {
        Annotation[] annotations = new Annotation[1];
        annotations[0] = MULTIPART_FORM;
        ContextProvidersCustomerForm form = get("/get/multipartform", ContextProvidersCustomerForm.class, annotations);
        ContextProvidersCustomer customer = form.getCustomer();
        Assertions.assertEquals("Bill", customer.getName(), "Wrong response");
        ContextProvidersCustomerFormNewAnnotationOnField form2 = get("/get/multipartform2",
                ContextProvidersCustomerFormNewAnnotationOnField.class, annotations);
        customer = form.getCustomer();
        Assertions.assertEquals("Bill", customer.getName(), "Wrong response");
        ContextProvidersCustomerFormNewAnnotationOnSetter form3 = get("/get/multipartform3",
                ContextProvidersCustomerFormNewAnnotationOnSetter.class, annotations);
        customer = form.getCustomer();
        Assertions.assertEquals("Bill", customer.getName(), "Wrong response");
    }

    /**
     * @tpTestDetails Xop data in get request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Xop")
    public void testGetXop() throws Exception {
        doTestGetXop();
    }

    void doTestGetXop() throws Exception {
        Annotation[] annotations = new Annotation[1];
        annotations[0] = XOP_WITH_MULTIPART_RELATED;
        ContextProvidersXop xop = get("/get/xop", ContextProvidersXop.class, annotations);
        Assertions.assertEquals("goodbye world", new String(xop.getBytes()), "Wrong response");
    }

    /**
     * @tpTestDetails Mixed data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Mixed")
    public void testPostMixed() throws Exception {
        doTestPostMixed();
    }

    @SuppressWarnings("unchecked")
    void doTestPostMixed() throws Exception {
        MultipartOutput output = new MultipartOutput();
        output.addPart(new ContextProvidersCustomer("Bill"), MediaType.APPLICATION_XML_TYPE);
        output.addPart("Bob", MediaType.TEXT_PLAIN_TYPE);
        Annotation[] annotations = new Annotation[1];
        annotations[0] = PART_TYPE_APPLICATION_XML;
        List<ContextProvidersName> names = new ArrayList<ContextProvidersName>();
        names = post("/post/mixed", output, MULTIPART_MIXED, names.getClass(), LIST_NAME_TYPE.getType(), annotations);
        Assertions.assertEquals(2, names.size());
        Assertions.assertTrue(names.contains(new ContextProvidersName("Bill")));
        Assertions.assertTrue(names.contains(new ContextProvidersName("Bob")));
    }

    /**
     * @tpTestDetails Form data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Form Data")
    public void testPostFormData() throws Exception {
        doTestPostFormData();
    }

    @SuppressWarnings("unchecked")
    public void doTestPostFormData() throws Exception {
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addFormData("bill", new ContextProvidersCustomer("Bill"), MediaType.APPLICATION_XML_TYPE);
        output.addFormData("bob", "Bob", MediaType.TEXT_PLAIN_TYPE);
        Annotation[] annotations = new Annotation[1];
        annotations[0] = PART_TYPE_APPLICATION_XML;
        List<ContextProvidersName> names = new ArrayList<ContextProvidersName>();
        names = post("/post/form", output, MULTIPART_FORM_DATA, names.getClass(), LIST_NAME_TYPE.getType(), annotations);
        Assert.assertThat("Wrong count of customers from response", new Integer(names.size()), is(2));
        Assert.assertThat("Received customers list do not contain all items", names,
                hasItems(new ContextProvidersName("Bill")));
        Assert.assertThat("Received customers list do not contain all items", names, hasItems(new ContextProvidersName("Bob")));
    }

    /**
     * @tpTestDetails List data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post List")
    public void testPostList() throws Exception {
        doTestPostList();
    }

    @SuppressWarnings("unchecked")
    public void doTestPostList() throws Exception {
        List<ContextProvidersCustomer> customers = new ArrayList<ContextProvidersCustomer>();
        customers.add(new ContextProvidersCustomer("Bill"));
        customers.add(new ContextProvidersCustomer("Bob"));
        Annotation[] annotations = new Annotation[1];
        annotations[0] = PART_TYPE_APPLICATION_XML;
        List<ContextProvidersName> names = new ArrayList<ContextProvidersName>();
        names = post("/post/list", customers, MULTIPART_MIXED, names.getClass(), LIST_NAME_TYPE.getType(), annotations);
        Assert.assertThat("Wrong count of customers from response", new Integer(names.size()), is(2));
        Assert.assertThat("Received customers list do not contain all items", names,
                hasItems(new ContextProvidersName("Bill")));
        Assert.assertThat("Received customers list do not contain all items", names, hasItems(new ContextProvidersName("Bob")));
    }

    /**
     * @tpTestDetails Map data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Map")
    public void testPostMap() throws Exception {
        doTestPostMap();
    }

    @SuppressWarnings("unchecked")
    public void doTestPostMap() throws Exception {
        Map<String, ContextProvidersCustomer> customers = new HashMap<String, ContextProvidersCustomer>();
        customers.put("bill", new ContextProvidersCustomer("Bill"));
        customers.put("bob", new ContextProvidersCustomer("Bob"));
        Annotation[] annotations = new Annotation[1];
        annotations[0] = PART_TYPE_APPLICATION_XML;
        List<ContextProvidersName> names = new ArrayList<ContextProvidersName>();
        names = post("/post/map", customers, MULTIPART_FORM_DATA, names.getClass(), LIST_NAME_TYPE.getType(), annotations);
        Assert.assertThat("Wrong count of customers from response", new Integer(names.size()), is(2));
        Assert.assertThat("Received customers list do not contain all items", names,
                hasItems(new ContextProvidersName("bill:Bill")));
        Assert.assertThat("Received customers list do not contain all items", names,
                hasItems(new ContextProvidersName("bob:Bob")));
    }

    /**
     * @tpTestDetails Related data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Related")
    public void testPostRelated() throws Exception {
        doTestPostRelated();
    }

    @SuppressWarnings("unchecked")
    void doTestPostRelated() throws Exception {
        MultipartRelatedOutput output = new MultipartRelatedOutput();
        output.setStartInfo("text/html");
        output.addPart("Bill", new MediaType("image", "png"), "bill", "binary");
        output.addPart("Bob", new MediaType("image", "png"), "bob", "binary");
        Annotation[] annotations = new Annotation[1];
        annotations[0] = PART_TYPE_APPLICATION_XML;
        List<ContextProvidersName> names = new ArrayList<ContextProvidersName>();
        names = post("/post/related", output, MULTIPART_RELATED, names.getClass(), LIST_NAME_TYPE.getType(), annotations);
        Assert.assertThat("Wrong count of customers from response", new Integer(names.size()), is(2));
        Assert.assertThat("Received customers list do not contain all items", names,
                hasItems(new ContextProvidersName("Bill")));
        Assert.assertThat("Received customers list do not contain all items", names, hasItems(new ContextProvidersName("Bob")));
    }

    /**
     * @tpTestDetails Multipart form data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Multipart Form")
    public void testPostMultipartForm() throws Exception {
        doTestPostMultipartForm();
    }

    void doTestPostMultipartForm() throws Exception {
        Annotation[] annotations = new Annotation[1];
        annotations[0] = MULTIPART_FORM;
        ContextProvidersCustomerForm form = new ContextProvidersCustomerForm();
        form.setCustomer(new ContextProvidersCustomer("Bill"));
        String name = post("/post/multipartform", form, MULTIPART_FORM_DATA, String.class, null, annotations);
        Assertions.assertEquals("Bill", name, "Wrong response");
        ContextProvidersCustomerFormNewAnnotationOnField form2 = new ContextProvidersCustomerFormNewAnnotationOnField();
        form2.setCustomer(new ContextProvidersCustomer("Bill"));
        name = post("/post/multipartform2", form2, MULTIPART_FORM_DATA, String.class, null, annotations);
        Assertions.assertEquals("Bill", name, "Wrong response");
        ContextProvidersCustomerFormNewAnnotationOnSetter form3 = new ContextProvidersCustomerFormNewAnnotationOnSetter();
        form3.setCustomer(new ContextProvidersCustomer("Bill"));
        name = post("/post/multipartform3", form3, MULTIPART_FORM_DATA, String.class, null, annotations);
        Assertions.assertEquals("Bill", name, "Wrong response");
    }

    /**
     * @tpTestDetails Xop data in post request is used.
     * @tpPassCrit RE should be able to find contextual data of type: javax.ws.rs.ext.Providers.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Post Xop")
    public void testPostXop() throws Exception {
        doTestPostXop();
    }

    void doTestPostXop() throws Exception {
        ContextProvidersXop xop = new ContextProvidersXop("hello world".getBytes());
        Annotation[] annotations = new Annotation[1];
        annotations[0] = XOP_WITH_MULTIPART_RELATED;
        String s = post("/post/xop", xop, MULTIPART_RELATED, String.class, null, annotations);
        Assertions.assertEquals("hello world", s, "Wrong response");
    }

    <T> T get(String path, Class<T> clazz) throws Exception {
        return get(path, clazz, null);
    }

    <T> T get(String path, Class<T> clazz, Annotation[] annotations) throws Exception {
        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(PortProviderUtil.generateURL(path, ContextProvidersTest.class.getSimpleName()));
            Response response = target.request().get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            T entity = response.readEntity(clazz, annotations);
            client.close();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(TestUtil.getErrorMessageForKnownIssue("RESTEASY-1119"), e);
        }
    }

    @SuppressWarnings({ "unchecked" })
    <S, T> T post(String path, S payload, MediaType mediaType, Class<T> returnType, Type genericReturnType,
            Annotation[] annotations) throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(PortProviderUtil.generateURL(path, ContextProvidersTest.class.getSimpleName()));
        Entity<S> entity = Entity.entity(payload, mediaType, annotations);
        Response response = target.request().post(entity);
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        T result = null;
        if (genericReturnType != null) {
            result = response.readEntity(new GenericType<T>(genericReturnType));
        } else {
            result = response.readEntity(returnType);
        }
        client.close();
        return result;
    }

    @DisplayName("S 1")
    public abstract static class S1 extends AnnotationLiteral<PartType> implements PartType {

        private static final long serialVersionUID = 1L;
    }

    @DisplayName("S 2")
    public abstract static class S2 extends AnnotationLiteral<MultipartForm> implements MultipartForm {

        private static final long serialVersionUID = 1L;
    }

    @DisplayName("S 3")
    public abstract static class S3 extends AnnotationLiteral<XopWithMultipartRelated> implements XopWithMultipartRelated {

        private static final long serialVersionUID = 1L;
    }
}
