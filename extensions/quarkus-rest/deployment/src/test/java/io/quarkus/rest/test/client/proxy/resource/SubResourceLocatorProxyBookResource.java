package io.quarkus.rest.test.client.proxy.resource;

import javax.ws.rs.Path;

import io.quarkus.rest.test.client.proxy.SubResourceLocatorProxyTest;

@Path("/gulliverstravels")
public class SubResourceLocatorProxyBookResource implements SubResourceLocatorProxyTest.Book {
    public String getTitle() {
        return "Gulliver's Travels";
    }

    @Override
    public SubResourceLocatorProxyTest.Chapter getChapter(int number) {
        return new SubResourceLocatorProxyChapterResource(number);
    }
}
