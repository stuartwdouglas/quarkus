package io.quarkus.rest.test.providers.multipart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.test.providers.multipart.resource.GenericTypeResource;
import io.quarkus.rest.test.providers.multipart.resource.GenericTypeStringListReaderWriter;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Multipart provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for JBEAP-1795
 * @tpSince RESTEasy 3.0.16
 */
public class GenericTypeMultipartTest {
    public static final GenericType<List<String>> stringListType = new GenericType<List<String>>() {
    };

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    war.addClasses(TestUtil.class, PortProviderUtil.class);
                    return TestUtil.finishContainerPrepare(war, null, GenericTypeResource.class,
                            GenericTypeStringListReaderWriter.class);
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, GenericTypeMultipartTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails List is in request.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testGenericType() throws Exception {
        Client client = ClientBuilder.newBuilder().register(GenericTypeStringListReaderWriter.class).build();
        WebTarget target = client.target(generateURL("/test"));
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        List<String> list = new ArrayList<>();
        list.add("darth");
        list.add("sidious");
        output.addFormData("key", list, stringListType, MediaType.APPLICATION_XML_TYPE);
        Entity<MultipartFormDataOutput> entity = Entity.entity(output, MediaType.MULTIPART_FORM_DATA_TYPE);
        String response = target.request().post(entity, String.class);
        Assert.assertEquals("Wrong response content", "darth sidious ", response);
        client.close();
    }

}
