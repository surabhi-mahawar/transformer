package com.samagra.transformer.odk.openrosa;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Set;


public interface CaseInsensitiveHeaders {
    @Nullable
    Set<String> getHeaders();

    boolean containsHeader(String header);

    @Nullable String getAnyValue(String header);

    @Nullable List<String> getValues(String header);
}
