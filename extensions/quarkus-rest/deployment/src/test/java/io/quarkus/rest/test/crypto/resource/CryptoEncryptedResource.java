package io.quarkus.rest.test.crypto.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.resteasy.security.smime.EnvelopedInput;
import org.jboss.resteasy.security.smime.EnvelopedOutput;

import io.quarkus.rest.test.Assert;

@Path("/smime/encrypted")
public class CryptoEncryptedResource {
    @GET
    public EnvelopedOutput get() {
        EnvelopedOutput output = new EnvelopedOutput("hello world", "text/plain");
        output.setCertificate(CryptoCertResource.cert);
        return output;
    }

    @POST
    public void post(EnvelopedInput<String> input) {
        String str = input.getEntity(CryptoCertResource.privateKey, CryptoCertResource.cert);
        Assert.assertEquals("input", str);
    }
}
