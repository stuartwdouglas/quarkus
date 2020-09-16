package io.quarkus.rest.test.client.resource;

import javax.ws.rs.QueryParam;

import io.quarkus.rest.test.client.TimeoutTest;

public class TimeoutResource implements TimeoutTest.TimeoutResourceInterface {
    @Override
    public String get(@QueryParam("sleep") int sleep) throws Exception {
        Thread.sleep(sleep * 1000);
        return "OK";
    }
}
