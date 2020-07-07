package com.samagra.transformer.odk.persistance;

import com.samagra.transformer.odk.model.Form;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.GeneralSecurityException;

@NoArgsConstructor
public class JsonDB {
    //Actual location on disk for database files, process should have read-write permissions to this folder
    static String dbFilesLocation;
    //Java package name where POJO's are present
    static String baseScanPackage;
    //Optionally a Cipher object if you need Encryption
    static ICipher cipher;
    static JsonDBTemplate jsonDBTemplate;

    public static void setup() throws GeneralSecurityException, IOException {
        dbFilesLocation = "src/main/resources";
        //Java package name where POJO's are present
        baseScanPackage = "com.samagra.transformer.odk.model";
        //Optionally a Cipher object if you need Encryption
        cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        jsonDBTemplate = new JsonDBTemplate(dbFilesLocation, baseScanPackage, cipher);
        jsonDBTemplate.createCollection(Form.class);

    }
}
