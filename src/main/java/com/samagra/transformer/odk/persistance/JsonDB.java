package com.samagra.transformer.odk.persistance;

import com.samagra.transformer.odk.model.Form;
import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;

import java.security.GeneralSecurityException;

public class JsonDB {
    private static volatile JsonDB instance = new JsonDB();
    private JsonDBTemplate DB = null;

    private JsonDB() {
        String dbFilesLocation = "/tmp/db";
        String baseScanPackage = "com.samagra.transformer";
        ICipher cipher;
        try {
            cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
            DB = new JsonDBTemplate(dbFilesLocation, baseScanPackage, cipher);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public static JsonDB getInstance() {
        return instance;
    }

    public JsonDBTemplate getDB(){
        try {
            instance.DB.createCollection(Form.class);
        } catch (InvalidJsonDbApiUsageException e) {
            System.out.println("DB already exist");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return instance.DB;
    }
}
