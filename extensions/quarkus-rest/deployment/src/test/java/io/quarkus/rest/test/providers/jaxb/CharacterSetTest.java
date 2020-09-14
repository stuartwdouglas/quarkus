package io.quarkus.rest.test.providers.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.runtime.client.QuarkusRestWebTarget;
import io.quarkus.rest.test.providers.jaxb.resource.CharacterSetData;
import io.quarkus.rest.test.providers.jaxb.resource.CharacterSetResource;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Jaxb provider
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.0.16
 */
public class CharacterSetTest {

    private final String[] characterSets = { "US-ASCII", "UTF-8", "ISO-8859-1" };
    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);

                    return TestUtil.finishContainerPrepare(war, null, CharacterSetData.class, CharacterSetResource.class);
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
        return PortProviderUtil.generateURL(path, CharacterSetTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Tests if correct Variant is chosen for given combination of mediatype xml and charsets.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void variantSelection() throws URISyntaxException {
        assertCharset("/variant-selection");
    }

    private void assertCharset(String path) throws URISyntaxException {
        for (String characterSet : characterSets) {
            QuarkusRestWebTarget target = client.target(generateURL(path));
            Response response = target.request().accept("application/xml").header("Accept-Charset", characterSet).get();

            assertEquals("Status code", 200, response.getStatus());

            String contentType = response.getHeaders().getFirst("Content-Type").toString();
            String charsetPattern = "application/xml\\s*;\\s*charset\\s*=\\s*\"?" + characterSet + "\"?";
            String charsetErrorMessage = contentType + " does not match " + charsetPattern;
            assertTrue(charsetErrorMessage, contentType.matches(charsetPattern));

            String xml = response.readEntity(String.class);
            String encodingPattern = "<\\?xml[^>]*encoding\\s*=\\s*['\"]" + characterSet + "['\"].*";
            String encodingErrorMessage = xml + " does not match " + encodingPattern;
            assertTrue(encodingErrorMessage, xml.matches(encodingPattern));

            response.close();
        }
    }

}
