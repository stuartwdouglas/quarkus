package io.quarkus.rest.test.providers.jaxb;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Xml JAXB Context Finder Test")
public class XmlJAXBContextFinderTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            return TestUtil.finishContainerPrepare(war, null, BeanWrapper.class, FirstBean.class, SecondBean.class,
                    FirstTestResource.class, SecondTestResource.class, MyJAXBContextResolver.class);
        }
    });

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @DisplayName("Bean Wrapper")
    public static final class BeanWrapper {

        @XmlAnyElement(lax = true)
        private Object bean;

        public Object getBean() {
            return this.bean;
        }

        public void setBean(Object bean) {
            this.bean = bean;
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @DisplayName("First Bean")
    public static final class FirstBean {

        private String data;

        public String getData() {
            return this.data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @DisplayName("Second Bean")
    public static final class SecondBean {

        private String data;

        public String getData() {
            return this.data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    @Path("/firstTestResource")
    @Produces(MediaType.APPLICATION_XML)
    @DisplayName("First Test Resource")
    public static final class FirstTestResource {

        @GET
        public Response get() {
            FirstBean firstBean = new FirstBean();
            firstBean.setData("firstTestResource");
            BeanWrapper beanWrapper = new BeanWrapper();
            beanWrapper.setBean(firstBean);
            return Response.ok(beanWrapper).build();
        }
    }

    @Path("/secondTestResource")
    @Produces(MediaType.APPLICATION_XML)
    @DisplayName("Second Test Resource")
    public static final class SecondTestResource {

        @GET
        public Response get() {
            SecondBean secondBean = new SecondBean();
            secondBean.setData("secondTestResource");
            BeanWrapper beanWrapper = new BeanWrapper();
            beanWrapper.setBean(secondBean);
            return Response.ok(beanWrapper).build();
        }
    }

    @Provider
    @Produces(MediaType.APPLICATION_XML)
    @DisplayName("My JAXB Context Resolver")
    public static class MyJAXBContextResolver implements ContextResolver<JAXBContext> {

        private JAXBContext jaxbContext;

        @Override
        public JAXBContext getContext(Class<?> type) {
            if (this.jaxbContext == null) {
                try {
                    this.jaxbContext = JAXBContext.newInstance(BeanWrapper.class, FirstBean.class, SecondBean.class);
                } catch (JAXBException e) {
                }
            }
            return this.jaxbContext;
        }
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, XmlJAXBContextFinderTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails In the following test both firstWebTarget and secondWebTarget will share the same XmlJAXBContextFinder
     *                inherited from their shared parent configuration. We define and register a ContextResolver<JAXBContext>
     *                for each
     *                webTarget so that firstWebTarget and secondWebTarget have its own (respectively firstJaxbContextResolver
     *                and secondJaxbContextResolver).
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test")
    public void test() {
        Client client = ClientBuilder.newClient();
        try {
            // First webTarget
            WebTarget firstWebTarget = client.target(generateURL("/firstTestResource"));
            ContextResolver<JAXBContext> firstJaxbContextResolver = new ContextResolver<JAXBContext>() {

                private JAXBContext jaxbContext;

                @Override
                public JAXBContext getContext(Class<?> type) {
                    if (this.jaxbContext == null) {
                        try {
                            this.jaxbContext = JAXBContext.newInstance(BeanWrapper.class, FirstBean.class);
                        } catch (JAXBException e) {
                        }
                    }
                    return this.jaxbContext;
                }
            };
            Response firstResponse = firstWebTarget.register(firstJaxbContextResolver).request(MediaType.APPLICATION_XML_TYPE)
                    .get();
            BeanWrapper firstBeanWrapper = firstResponse.readEntity(BeanWrapper.class);
            Assertions.assertTrue(FirstBean.class.isAssignableFrom(firstBeanWrapper.getBean().getClass()),
                    "First bean is not assignable from the parent bean");
            // Second webTarget
            WebTarget secondWebTarget = client.target(generateURL("/secondTestResource"));
            // Will never be called
            ContextResolver<JAXBContext> secondJaxbContextResolver = new ContextResolver<JAXBContext>() {

                private JAXBContext jaxbContext;

                @Override
                public JAXBContext getContext(Class<?> type) {
                    if (this.jaxbContext == null) {
                        try {
                            this.jaxbContext = JAXBContext.newInstance(BeanWrapper.class, SecondBean.class);
                        } catch (JAXBException e) {
                        }
                    }
                    return this.jaxbContext;
                }
            };
            Response secondResponse = secondWebTarget.register(secondJaxbContextResolver)
                    .request(MediaType.APPLICATION_XML_TYPE).get();
            BeanWrapper secondBeanWrapper = secondResponse.readEntity(BeanWrapper.class);
            Assertions.assertTrue(SecondBean.class.isAssignableFrom(secondBeanWrapper.getBean().getClass()),
                    "Second bean is not assignable from the parent bean");
        } finally {
            client.close();
        }
    }
}
