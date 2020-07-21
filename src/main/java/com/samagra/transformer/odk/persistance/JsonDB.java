package com.samagra.transformer.odk.persistance;

import com.samagra.transformer.odk.model.Form;
import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.GeneralSecurityException;

@NoArgsConstructor
public class JsonDB {

    public static JsonDBTemplate setupDatabase() throws GeneralSecurityException {
        String dbFilesLocation = "/tmp/db";
        String baseScanPackage = "com.samagra.transformer.odk.model.Form";
        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesLocation, baseScanPackage, cipher);
        try{
            jsonDBTemplate.createCollection(Form.class);
        }catch (InvalidJsonDbApiUsageException e){
            System.out.println("DB already exist");
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return jsonDBTemplate;
    }
}
