package com.samagra.transformer.application;

import android.webkit.MimeTypeMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.transformer.odk.*;
import com.samagra.transformer.odk.model.Form;
import com.samagra.transformer.odk.model.FormDetails;
import com.samagra.transformer.odk.openrosa.CollectThenSystemContentTypeMapper;
import com.samagra.transformer.odk.openrosa.OpenRosaAPIClient;
import com.samagra.transformer.odk.openrosa.OpenRosaHttpInterface;
import com.samagra.transformer.odk.openrosa.okhttp.OkHttpConnection;
import com.samagra.transformer.odk.openrosa.okhttp.OkHttpOpenRosaServerClientProvider;
import com.samagra.transformer.odk.persistance.FormsDao;
import com.samagra.transformer.odk.persistance.JsonDB;
import com.samagra.transformer.odk.utilities.FormListDownloader;
import com.samagra.transformer.odk.utilities.WebCredentialsUtils;
import io.jsondb.JsonDBTemplate;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PostConstruct;
import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableKafka
@EnableAsync
@ComponentScan(basePackages = {"com.samagra.transformer"})
@EnableJpaRepositories(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer"})
@EntityScan(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer"})
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@SpringBootApplication()
@Slf4j
public class TransformerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TransformerApplication.class, args);
    }

    @PostConstruct
    private void postConstruct() {
        // downloadForms();
        // testFormManager();
    }

    private void testFormManager() {
        String formPath = ODKTransformer.getFormPath("practice_form");
        ServiceResponse response1 = new FormManager(null, null, null, formPath).start();
        log.debug("First response");
        log.debug(response1.getCurrentIndex(), response1.getNextMessage());
    }

    private void downloadForms() {
        //Empty the database and folder
        FormsDao dao;
        try{
            File directoryToDelete = new File("/tmp/forms");
            FileSystemUtils.deleteRecursively(directoryToDelete);
            dao = new FormsDao(JsonDB.setupDatabase());
            dao.deleteFormsDatabase();
        }catch (Exception e){}

        //Create a folder /tmp/forms
        new File("/tmp/forms").mkdirs();

        //Download fresh
        OpenRosaHttpInterface openRosaHttpInterface = new OkHttpConnection(
                new OkHttpOpenRosaServerClientProvider(new OkHttpClient()),
                null,
                "userAgent"
        );
        WebCredentialsUtils webCredentialsUtils = new WebCredentialsUtils();
        OpenRosaAPIClient openRosaAPIClient = new OpenRosaAPIClient(openRosaHttpInterface, webCredentialsUtils);
        FormListDownloader formListDownloader = new FormListDownloader(
                openRosaAPIClient,
                webCredentialsUtils);
        HashMap<String, FormDetails> formList = formListDownloader.downloadFormList(false);
        int count = 0;
        if (formList.size() > 0) {
            ArrayList<FormDetails> forms = new ArrayList<>();
            for (Map.Entry<String, FormDetails> form : formList.entrySet()) {
                forms.add(form.getValue());
                count += 1;
            }
            FormDownloader formDownloader = null;
            try {
                dao = new FormsDao(JsonDB.setupDatabase());
                formDownloader = new FormDownloader(dao, openRosaAPIClient);
                formDownloader.downloadForms(forms);
                List<Form> downloadedForms =  dao.getForms();
                log.info("Total downloaded forms: " + downloadedForms.size());

            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }
    }
}

