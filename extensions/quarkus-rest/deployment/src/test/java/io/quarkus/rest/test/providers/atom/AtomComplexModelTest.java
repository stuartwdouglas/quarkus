package io.quarkus.rest.test.providers.atom;

import static junit.framework.TestCase.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.DataOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Person;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.runtime.client.QuarkusRestClient;
import io.quarkus.rest.test.providers.atom.resource.AtomAssetMetadata;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelArchived;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelAtomAssetMetadataDecorators;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelAtomAssetMetadtaProcessor;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelCategories;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelCheckinComment;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelCreated;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelDisabled;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelEntryResource;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelFormat;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelNote;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelState;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelUuid;
import io.quarkus.rest.test.providers.atom.resource.AtomComplexModelVersionNumber;
import io.quarkus.rest.test.simple.PortProviderUtil;
import io.quarkus.rest.test.simple.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Atom provider
 * @tpChapter Integration tests
 * @tpTestCaseDetails Check complex model with Atom Provider
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Atom Complex Model Test")
public class AtomComplexModelTest {

    static QuarkusRestClient client;

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {

        @Override
        public JavaArchive get() {
            JavaArchive war = ShrinkWrap.create(JavaArchive.class);
            war.addClasses(PortProviderUtil.class);
            war.addClasses(AtomComplexModelArchived.class, AtomAssetMetadata.class,
                    AtomComplexModelAtomAssetMetadataDecorators.class, AtomComplexModelAtomAssetMetadtaProcessor.class,
                    AtomComplexModelCategories.class, AtomComplexModelCheckinComment.class, AtomComplexModelCreated.class,
                    AtomComplexModelDisabled.class, AtomComplexModelEntryResource.class, AtomComplexModelFormat.class,
                    AtomComplexModelNote.class, AtomComplexModelState.class, AtomComplexModelUuid.class,
                    AtomComplexModelVersionNumber.class);
            return TestUtil.finishContainerPrepare(war, null, AtomComplexModelEntryResource.class);
        }
    });

    @BeforeEach
    public void init() {
        client = (QuarkusRestClient) ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, AtomComplexModelTest.class.getSimpleName());
    }

    /**
     * @tpTestDetails Check complex type
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Complex Type")
    public void testComplexType() throws Exception {
        URI baseUri = new URI("resteasy-test");
        Entry entry = new Entry();
        entry.setTitle("testtitle");
        entry.setSummary("testdesc");
        entry.setPublished(new Date());
        entry.getAuthors().add(new Person("testperson"));
        entry.setId(baseUri);
        AtomAssetMetadata atomAssetMetadata = entry.getAnyOtherJAXBObject(AtomAssetMetadata.class);
        if (atomAssetMetadata == null) {
            atomAssetMetadata = new AtomAssetMetadata();
        }
        atomAssetMetadata.setArchived(false);
        atomAssetMetadata.setUuid("testuuid");
        atomAssetMetadata.setCategories(new String[] { "a", "b", "c" });
        entry.setAnyOtherJAXBObject(atomAssetMetadata);
        Content content = new Content();
        content.setSrc(UriBuilder.fromUri(baseUri).path("binary").build());
        content.setType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        entry.setContent(content);
        Class[] classes = new Class[] { AtomAssetMetadata.class, Entry.class };
        JAXBContext jaxbContext = JAXBContext.newInstance(classes);
        Marshaller marshaller = jaxbContext.createMarshaller();
        Writer xmlWriter = new StringWriter();
        marshaller.marshal(entry, xmlWriter);
        String xmlOut = xmlWriter.toString();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StringReader xmlReader = new StringReader(xmlOut);
        Entry newEntry = (Entry) unmarshaller.unmarshal(xmlReader);
        atomAssetMetadata = newEntry.getAnyOtherJAXBObject(AtomAssetMetadata.class);
        assertNotNull(atomAssetMetadata, "Metadata of complex type is null");
        assertNotNull(atomAssetMetadata.getCategories(), "Categories from metadata is missing");
    }

    /**
     * @tpTestDetails Check new client
     * @tpInfo Not for forward compatibility due to 3.1.0.Final, see the migration notes
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test New Client")
    public void testNewClient() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<entry xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>testCreatePackageFromAtom7</title>"
                + "<summary>desc for testCreatePackageFromAtom</summary>"
                + "<metadata xmlns=\"\"><categories><value>c1</value></categories> <note><value>meta</value> </note></metadata>"
                + "</entry>";
        {
            URL url = new URL(generateURL("/entry"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_ATOM_XML);
            connection.setRequestProperty("Content-Length", Integer.toString(xml.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            assertEquals(Status.OK.getStatusCode(), connection.getResponseCode());
        }
        {
            Response response = client.target(generateURL("/entry2")).request().header("Accept", MediaType.APPLICATION_ATOM_XML)
                    .header("Content-Type", MediaType.APPLICATION_ATOM_XML).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertNotNull(response.readEntity(Entry.class).getAnyOtherJAXBObject(AtomAssetMetadata.class),
                    "Wrong content of response");
            response.close();
        }
        {
            URL url = new URL(generateURL("/entry3"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
            connection.setRequestProperty("Content-Length", Integer.toString(xml.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            // Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(xml);
            wr.flush();
            wr.close();
            assertEquals(Status.OK.getStatusCode(), connection.getResponseCode());
        }
        {
            Response response = client.target(generateURL("/entry4")).request().header("Accept", MediaType.APPLICATION_XML)
                    .header("Content-Type", MediaType.APPLICATION_XML).get();
            Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertNotNull(response.readEntity(Entry.class).getAnyOtherJAXBObject(AtomAssetMetadata.class),
                    "Wrong content of response");
            response.close();
        }
    }
}
