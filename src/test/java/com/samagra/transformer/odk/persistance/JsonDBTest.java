package com.samagra.transformer.odk.persistance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=JsonDB.class)
@EnableConfigurationProperties
public class JsonDBTest {

    @Test
    public void testJSONDBSetup() throws GeneralSecurityException, IOException {
        JsonDB.setup();
        System.out.println("cfcfv " + JsonDB.dbFilesLocation);
    }
}