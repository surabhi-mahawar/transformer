package com.samagra.transformer.odk.model;

import java.io.Serializable;

public class FormDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String errorStr;
    private String formName;
    private String downloadUrl;
    private String manifestUrl;
    private String formID;
    private String formVersion;
    private String hash;
    private String manifestFileHash;
    private boolean isNewerFormVersionAvailable;
    private boolean areNewerMediaFilesAvailable;

    public FormDetails(String error) {
        errorStr = error;
    }

    public FormDetails(String formName, String downloadUrl, String manifestUrl, String formID,
                       String formVersion, String hash, String manifestFileHash,
                       boolean isNewerFormVersionAvailable, boolean areNewerMediaFilesAvailable) {
        this.formName = formName;
        this.downloadUrl = downloadUrl;
        this.manifestUrl = manifestUrl;
        this.formID = formID;
        this.formVersion = formVersion;
        this.hash = hash;
        this.manifestFileHash = manifestFileHash;
        this.isNewerFormVersionAvailable = isNewerFormVersionAvailable;
        this.areNewerMediaFilesAvailable = areNewerMediaFilesAvailable;
    }

    public String getErrorStr() {
        return errorStr;
    }

    public String getFormName() {
        return formName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public String getFormId() {
        return formID;
    }

    public String getFormVersion() {
        return formVersion;
    }

    public String getHash() {
        return hash;
    }

    public String getManifestFileHash() {
        return manifestFileHash;
    }

    public boolean isNewerFormVersionAvailable() {
        return isNewerFormVersionAvailable;
    }

    public boolean areNewerMediaFilesAvailable() {
        return areNewerMediaFilesAvailable;
    }
}
