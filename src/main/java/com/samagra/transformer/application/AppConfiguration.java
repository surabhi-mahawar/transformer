package com.samagra.transformer.application;

import com.samagra.transformer.odk.model.Form;
import com.samagra.transformer.odk.persistance.FormsDao;
import io.jsondb.InvalidJsonDbApiUsageException;
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

//    @Bean
//    public FormsDao getFormsDao(){
//        return new FormsDao();
//    }
}
