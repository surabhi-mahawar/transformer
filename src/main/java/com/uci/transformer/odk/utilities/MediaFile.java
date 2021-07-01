package com.uci.transformer.odk.utilities;

public class MediaFile {
    private final String filename;
    private final String hash;
    private final String downloadUrl;

    public MediaFile(String filename, String hash, String downloadUrl) {
        this.filename = filename;
        this.hash = hash;
        this.downloadUrl = downloadUrl;
    }

    public String getFilename() {
        return filename;
    }

    public String getHash() {
        return hash;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}