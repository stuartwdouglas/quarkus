package io.quarkus.rest.test.resource.param;

import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.util.MultivaluedMapImpl;
import io.quarkus.rest.test.resource.param.resource.CookieParamWrapper;
import io.quarkus.rest.test.resource.param.resource.CookieParamWrapperArrayConverter;
import io.quarkus.rest.test.resource.param.resource.DateParamConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedCookieParam;
import io.quarkus.rest.test.resource.param.resource.MultiValuedCookieParamConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParam;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamConverter;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamConverterProvider;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamResource;
import io.quarkus.rest.test.resource.param.resource.MultiValuedParamResourceClient;
import io.quarkus.rest.test.resource.param.resource.MultiValuedPathParam;
import io.quarkus.rest.test.resource.param.resource.MultiValuedPathParamConverter;
import io.quarkus.rest.test.resource.param.resource.ParamWrapper;
import io.quarkus.rest.test.resource.param.resource.ParamWrapperArrayConverter;
import io.quarkus.rest.test.resource.param.resource.PathParamWrapper;
import io.quarkus.rest.test.resource.param.resource.PathParamWrapperArrayConverter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Parameters
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test for RESTEasy extended support for multivalue @*Param (RESTEASY-1566 + RESTEASY-1746)
 *                    java.util.Date class is used
 *                    Client Proxy is not used
 * @tpSince RESTEasy 4.0.0
 */
public class MultiValuedParamDateNoProxyTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClass(MultiValuedParam.class);
                    war.addClass(ParamWrapper.class);
                    war.addClass(MultiValuedCookieParam.class);
                    war.addClass(CookieParamWrapper.class);
                    war.addClass(MultiValuedPathParam.class);
                    war.addClass(PathParamWrapper.class);
                    war.addClass(DateParamConverter.class);
                    war.addClass(MultiValuedParamConverter.class);
                    war.addClass(ParamWrapperArrayConverter.class);
                    war.addClass(MultiValuedCookieParamConverter.class);
                    war.addClass(CookieParamWrapperArrayConverter.class);
                    war.addClass(MultiValuedPathParamConverter.class);
                    war.addClass(PathParamWrapperArrayConverter.class);
                    war.addClass(MultiValuedParamResourceClient.class);
                    return TestUtil.finishContainerPrepare(war, null, MultiValuedParamConverterProvider.class,
                            MultiValuedParamResource.class, MultiValuedParamResource.QueryParamResource.class,
                            MultiValuedParamResource.HeaderParamResource.class,
                            MultiValuedParamResource.PathParamResource.class,
                            MultiValuedParamResource.CookieParamResource.class,
                            MultiValuedParamResource.MatrixParamResource.class,
                            MultiValuedParamResource.FormParamResource.class);
                }
            });

    private String generateBaseUrl() {
        return PortProviderUtil.generateBaseUrl(MultiValuedParamDateNoProxyTest.class.getSimpleName());
    }

    /**
     * Define testcase data set
     */
    String date1 = "20161217";
    String date2 = "20161218";
    String date3 = "20161219";
    String expectedResponse = date1 + "," + date2 + "," + date3;
    final MultivaluedMap<String, Object> dates = new MultivaluedMapImpl<>();

    public MultiValuedParamDateNoProxyTest() {
        dates.add("date", date1);
        dates.add("date", date2);
        dates.add("date", date3);
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testQueryParam() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;
            response = client.target(generateBaseUrl() + "/queryParam/customConversion_multiValuedParam")
                    .queryParam("date", date1 + "," + date2 + "," + date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/customConversion_multiValuedParam_array")
                    .queryParam("date", date1 + "," + date2 + "," + date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_list")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_arrayList")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_set")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_hashSet")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_sortedSet")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_treeSet")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/queryParam/defaultConversion_array")
                    .queryParams(dates).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testHeaderParam() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;

            response = client.target(generateBaseUrl() + "/headerParam/customConversion_multiValuedParam")
                    .request().header("date", date1 + "," + date2 + "," + date3).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/customConversion_multiValuedParam_array")
                    .request().header("date", date1 + "," + date2 + "," + date3).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_list")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_arrayList")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_set")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_hashSet")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_sortedSet")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_treeSet")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/headerParam/defaultConversion_array")
                    .request().headers(dates).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testMatrixParam() {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;
            response = client.target(generateBaseUrl() + "/matrixParam/customConversion_multiValuedParam")
                    .matrixParam("date", date1 + "," + date2 + "," + date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/customConversion_multiValuedParam_array")
                    .matrixParam("date", date1 + "," + date2 + "," + date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_list")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_arrayList")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_set")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_hashSet")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_sortedSet")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_treeSet")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/matrixParam/defaultConversion_array")
                    .matrixParam("date", date1).matrixParam("date", date2).matrixParam("date", date3).request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testCookieParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;
            response = client.target(generateBaseUrl() + "/cookieParam/customConversion_multiValuedCookieParam")
                    .request().cookie("date", date1 + "-" + date2 + "-" + date3).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/customConversion_multiValuedCookieParam_array")
                    .request().cookie("date", date1 + "-" + date2 + "-" + date3).get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_list")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_arrayList")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));

            response.close();
            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_set")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_hashSet")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_sortedSet")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_treeSet")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/cookieParam/defaultConversion_array")
                    .request().cookie("date", date1).get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testFormParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;
            Form form = new Form();
            form.param("date", date1 + "," + date2 + "," + date3);
            Entity<Form> entity = Entity.form(form);

            response = client.target(generateBaseUrl() + "/formParam/customConversion_multiValuedParam")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(entity);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/customConversion_multiValuedParam_array")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(entity);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            Form datesForm = new Form();
            datesForm.param("date", date1).param("date", date2).param("date", date3);
            Entity<Form> dates = Entity.form(datesForm);

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_list")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_arrayList")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_set")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_hashSet")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_sortedSet")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_treeSet")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/formParam/defaultConversion_array")
                    .request(MediaType.APPLICATION_FORM_URLENCODED).post(dates);
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

    /**
     * @tpTestDetails Set specific values
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testPathParam() throws Exception {
        QuarkusRestClient client = (QuarkusRestClient) ClientBuilder.newClient();
        try {
            Response response;
            response = client
                    .target(generateBaseUrl() + "/pathParam/customConversion_multiValuedPathParam/20161217/20161218/20161219")
                    .request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client
                    .target(generateBaseUrl()
                            + "/pathParam/customConversion_multiValuedPathParam_array/20161217/20161218/20161219")
                    .request().get();
            Assert.assertEquals(expectedResponse, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_list/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_arrayList/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_set/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_hashSet/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_sortedSet/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_treeSet/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

            response = client.target(generateBaseUrl() + "/pathParam/defaultConversion_array/" + date1)
                    .request().get();
            Assert.assertEquals(date1, response.readEntity(String.class));
            response.close();

        } finally {
            client.close();
        }
    }

}
