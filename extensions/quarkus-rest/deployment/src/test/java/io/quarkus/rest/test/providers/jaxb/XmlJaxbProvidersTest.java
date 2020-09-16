package io.quarkus.rest.test.providers.jaxb;

import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.jaxb.resource.Item;
import io.quarkus.rest.test.providers.jaxb.resource.Itemtype;
import io.quarkus.rest.test.providers.jaxb.resource.JAXBCache;
import io.quarkus.rest.test.providers.jaxb.resource.Order;
import io.quarkus.rest.test.providers.jaxb.resource.Ordertype;
import io.quarkus.rest.test.providers.jaxb.resource.ShipTo;
import io.quarkus.rest.test.providers.jaxb.resource.Shiptotype;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJaxbProvidersHelper;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJaxbProvidersOrderClient;
import io.quarkus.rest.test.providers.jaxb.resource.XmlJaxbProvidersOrderResource;
import io.quarkus.rest.test.providers.jaxb.resource.XmlStreamFactory;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xml Jaxb Providers Test")
public class XmlJaxbProvidersTest {

    private XmlJaxbProvidersOrderClient proxy;

    static QuarkusRestClient client;

    private static final String ERR_NULL_ENTITY = "The entity returned from the server was null";

    private static final String ERR_CONTENT = "Unexpected content of the Order";

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClass(XmlJaxbProvidersTest.class);
            war.addAsResource(XmlJaxbProvidersTest.class.getPackage(), "orders/order_123.xml");
            war.as(ZipExporter.class).exportTo(new File("target", XmlJaxbProvidersTest.class.getSimpleName() + ".war"), true);
            return TestUtil.finishContainerPrepare(war, null, XmlJaxbProvidersOrderResource.class, Order.class, Ordertype.class,
                    ShipTo.class, Shiptotype.class, Item.class, Itemtype.class, JAXBCache.class, XmlJaxbProvidersHelper.class,
                    XmlStreamFactory.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
        proxy = ProxyBuilder.builder(XmlJaxbProvidersOrderClient.class, client.target(generateURL("/"))).build();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, XmlJaxbProvidersTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Test jaxb unmarshaller to correctly unmarshall InputStream
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Unmarshal Order")
    public void testUnmarshalOrder() throws Exception {
        InputStream in = XmlJaxbProvidersTest.class.getResourceAsStream("orders/order_123.xml");
        Order order = XmlJaxbProvidersHelper.unmarshall(Order.class, in).getValue();
        Assertions.assertNotNull(ERR_NULL_ENTITY, order);
        Assertions.assertEquals(ERR_CONTENT, "Ryan J. McDonough", order.getPerson());
    }

    /**
     * @tpTestDetails An xml file is loaded on the server and jaxb converts the xml entity Order from xml file into an
     *                object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Order")
    public void testGetOrder() {
        Order order = proxy.getOrderById("order_123");
        Assertions.assertEquals(ERR_CONTENT, "Ryan J. McDonough", order.getPerson());
    }

    /**
     * @tpTestDetails Clients sends request with order if and set xml headerr. An xml file is loaded on the server
     *                and jaxb converts the xml entity Order from xml file into an object.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Order And Unmarshal")
    public void testGetOrderAndUnmarshal() throws Exception {
        Response response = client.target(generateURL("/jaxb/orders") + "/order_123").request()
                .header(XmlJaxbProvidersHelper.FORMAT_XML_HEADER, "true").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JAXBContext jaxb = JAXBContext.newInstance(Order.class);
        Unmarshaller u = jaxb.createUnmarshaller();
        Order order = (Order) u.unmarshal(response.readEntity(InputStream.class));
        Assertions.assertNotNull(ERR_NULL_ENTITY, order);
        Assertions.assertEquals(ERR_CONTENT, "Ryan J. McDonough", order.getPerson());
        response.close();
    }

    /**
     * @tpTestDetails Same as testGetOrderWithParams() except that it uses the client framework to implicitly unmarshal
     *                the returned order and it tests its value, instead of just printing it out.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Get Order With Params To Order")
    public void testGetOrderWithParamsToOrder() throws Exception {
        Response response = client.target(generateURL("/jaxb/orders") + "/order_123").request()
                .header(XmlJaxbProvidersHelper.FORMAT_XML_HEADER, "true").get();
        Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Order order = response.readEntity(Order.class);
        Assertions.assertEquals(ERR_CONTENT, "Ryan J. McDonough", order.getPerson());
    }

    /**
     * @tpTestDetails Updates the specified order and returns updated object
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Update Order")
    public void testUpdateOrder() {
        InputStream in = XmlJaxbProvidersTest.class.getResourceAsStream("orders/order_123.xml");
        Order order = XmlJaxbProvidersHelper.unmarshall(Order.class, in).getValue();
        int initialItemCount = order.getItems().size();
        order = proxy.updateOrder(order, "order_123");
        Assertions.assertEquals(ERR_CONTENT, "Ryan J. McDonough", order.getPerson());
        Assert.assertNotSame("The number of items in the Order didn't change after update", initialItemCount,
                order.getItems().size());
        Assertions.assertEquals(3, order.getItems().size(), "The number of items in the Order doesn't match");
    }
}
