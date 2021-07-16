package com.uci.transformer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.transformer.odk.entity.converters.AssessmentWriteConverter;
import com.uci.utils.PSQL.JsonToMapConverter;
import com.uci.utils.PSQL.MapToJsonConverter;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories
public class PostgresConfig extends AbstractR2dbcConfiguration {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                .host("homeserver")
                .port(5435)
                .username("postgresql")
                .password("yoursupersecret")
                .database("formsdb")
                .build());
    }

    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?,?>> converters = new ArrayList<>();
        converters.add(new JsonToMapConverter(objectMapper));
        converters.add(new MapToJsonConverter(objectMapper));
        converters.add(new AssessmentWriteConverter());
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }
}