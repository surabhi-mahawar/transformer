package com.uci.transformer.odk.persistance;

import io.jsondb.JsonDBTemplate;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class JsonDBTest {

    @Test
    public void testJSONDBSetup() throws GeneralSecurityException, IOException {
        JsonDBTemplate jsonDBTemplate = JsonDB.getInstance().getDB();
        System.out.println("cfcfv " + jsonDBTemplate);
    }
}