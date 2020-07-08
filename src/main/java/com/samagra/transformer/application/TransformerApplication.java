package com.samagra.transformer.application;

import com.samagra.transformer.odk.FormDownloader;
import com.samagra.transformer.odk.openrosa.OpenRosaAPIClient;
import com.samagra.transformer.odk.openrosa.okhttp.OkHttpConnection;
import com.samagra.transformer.odk.utilities.FormListDownloader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableKafka
@EnableAsync
@ComponentScan(basePackages = {"com.samagra.transformer", "com.samagra"})
@EnableJpaRepositories(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer"})
@EntityScan(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity", "com.samagra.transformer"})
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@SpringBootApplication()
public class TransformerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransformerApplication.class, args);

    }

}
