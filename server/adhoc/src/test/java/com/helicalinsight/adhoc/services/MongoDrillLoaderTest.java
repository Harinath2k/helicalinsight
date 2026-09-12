package com.helicalinsight.adhoc.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MongoDrillLoaderTest {

    @Test
    public void preservesSrvUriOptionsAndAddsFormCredentials() {
        String uri = MongoDrillLoader.createConnectionString(
                "mongodb+srv://cluster.example.test/analytics?retryWrites=true&w=majority",
                "report user",
                "p@ssword");

        assertEquals(
                "mongodb+srv://report%20user:p%40ssword@cluster.example.test/analytics?retryWrites=true&w=majority",
                uri);
    }

    @Test
    public void preservesCredentialsAlreadyIncludedInUri() {
        String uri = "mongodb://existing:secret@localhost:27017/analytics";

        assertEquals(uri, MongoDrillLoader.createConnectionString(uri, "form-user", "form-password"));
    }
}