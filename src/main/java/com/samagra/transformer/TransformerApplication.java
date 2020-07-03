package com.samagra.transformer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableKafka
@EnableAsync
@ComponentScan(basePackages = {"com.samagra.*","com.*"})
@EnableJpaRepositories(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity"})
@EntityScan(basePackages = {"messagerosa.dao", "com.samagra.transformer.odk.entity"})
@PropertySource("application-messagerosa.properties")
@PropertySource("application.properties")
@SpringBootApplication()
public class TransformerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransformerApplication.class, args);
    }

}
