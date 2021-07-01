package com.uci.transformer.odk.utilities;

import java.util.List;

public class ManifestFile {
    private final String hash;
    private final List<MediaFile> mediaFiles;

    public ManifestFile(String hash, List<MediaFile> mediaFiles) {
        this.hash = hash;
        this.mediaFiles = mediaFiles;
    }

    public String getHash() {
        return hash;
    }

    public List<MediaFile> getMediaFiles() {
        return mediaFiles;
    }
}
