package com.samagra.transformer.odk.openrosa;

import org.springframework.lang.Nullable;

public interface OpenRosaServerClientProvider {

    OpenRosaServerClient get(String schema, String userAgent, @Nullable HttpCredentialsInterface credentialsInterface);
}
