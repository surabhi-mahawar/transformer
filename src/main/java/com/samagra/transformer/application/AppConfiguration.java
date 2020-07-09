package com.samagra.transformer.application;

import com.samagra.transformer.odk.model.Form;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Configuration
@EnableAutoConfiguration
public class AppConfiguration {

    @Bean
    public JsonDBTemplate setupDatabase() throws GeneralSecurityException {
        String dbFilesLocation = "src/main/resources";
        String baseScanPackage = "com.samagra.transformer.odk.model.Form";
        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesLocation, baseScanPackage, cipher);
        jsonDBTemplate.createCollection(Form.class);
        return jsonDBTemplate;
    }
}
