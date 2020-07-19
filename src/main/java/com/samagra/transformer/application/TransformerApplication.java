package com.samagra.transformer.application;

import android.webkit.MimeTypeMap;
import com.samagra.transformer.odk.FormDownloader;
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

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableKafka
@EnableAsync
@ComponentScan(basePackages = {"com.samagra.transformer", "com.samagra"})
@EnableJpaRepositories(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer"})
@EntityScan(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer", "com.samagra.orchestrator"})
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@SpringBootApplication()
public class TransformerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TransformerApplication.class, args);
    }

    @PostConstruct
    private void downloadForms() {
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
                FormsDao dao = new FormsDao(JsonDB.setupDatabase());
                formDownloader = new FormDownloader(dao, openRosaAPIClient);
                formDownloader.downloadForms(forms);
                List<Form> downloadedForms =  dao.getForms();
                System.out.println(downloadedForms);

            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }



        }
    }
}

