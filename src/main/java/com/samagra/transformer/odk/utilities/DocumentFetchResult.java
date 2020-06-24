package com.samagra.transformer.odk.utilities;

import org.kxml2.kdom.Document;

public class DocumentFetchResult {
    public final String errorMessage;
    public final int responseCode;
    public final Document doc;
    public final boolean isOpenRosaResponse;
    private String hash;

    public DocumentFetchResult(String msg, int response) {
        responseCode = response;
        errorMessage = msg;
        doc = null;
        isOpenRosaResponse = false;
    }

    public DocumentFetchResult(Document doc, boolean isOpenRosaResponse, String hash) {
        responseCode = 0;
        errorMessage = null;
        this.doc = doc;
        this.isOpenRosaResponse = isOpenRosaResponse;
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }
}