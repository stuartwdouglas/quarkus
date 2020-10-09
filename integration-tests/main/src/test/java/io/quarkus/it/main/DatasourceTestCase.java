package io.quarkus.it.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.datasource.DatasourceResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.web.WebTest;

@QuarkusTest
@TestHTTPEndpoint(DatasourceResource.class)
public class DatasourceTestCase {

    @Test
    @WebTest
    public void testDataSource(String body) {
        Assertions.assertEquals("10", body);
    }

    @Test
    @WebTest("/txn")
    public void testDataSourceTransactions(String body) {
        Assertions.assertEquals("PASSED", body);
    }

}
