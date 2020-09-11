package com.samagra.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class DbSchedulerJsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        Optional<String> objectAsString = Optional.empty();

        try {
            objectAsString = Optional.ofNullable(mapper.writeValueAsString(o));
        } catch (JsonProcessingException ignored) {
        }

        return objectAsString.isEmpty() ? null : objectAsString.get().getBytes();
    }

    @Override
    public <T> T deserialize(Class<T> aClass, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        T o;

        @SuppressWarnings("rawtypes")
        Map aHashMap = null;

        try {
            aHashMap = mapper.readValue(new String(bytes), Map.class);
        } catch (IOException ignored) {
        }

        o = mapper.convertValue(aHashMap, aClass);

        return o;
    }
}
