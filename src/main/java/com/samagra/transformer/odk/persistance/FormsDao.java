package com.samagra.transformer.odk.persistance;

import android.content.ContentValues;
import android.database.Cursor;

import com.samagra.transformer.odk.model.Form;
import com.samagra.transformer.odk.utilities.FileUtils;
import io.jsondb.JsonDBTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FormsDao {

    @Autowired
    JsonDBTemplate jsonDBTemplate;

    public List<Form> getFormsCursor() {
        return getFormsCursor(null, null, null, null);
    }

    public List<Form> getFormsCursor(String selection, String[] selectionArgs) {
        return getFormsCursor(null, selection, selectionArgs, null);
    }

    List<Form> getFormsCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    public List<Form> getFormsCursor(String formId, String formVersion) {
        return null;
    }

    public Cursor getFormsCursorForFormId(String formId) {
        return null;
//        String selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=?";
//        String[] selectionArgs = {formId};
//
//        return getFormsCursor(null, selection, selectionArgs, null);
    }

    public String getFormTitleForFormIdAndFormVersion(String formId, String formVersion) {
//        String formTitle = "";
//
//        Cursor cursor = getFormsCursor(formId, formVersion);
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    formTitle = cursor.getString(cursor.getColumnIndex(FormsProviderAPI.FormsColumns.DISPLAY_NAME));
//                }
//            } finally {
//                cursor.close();
//            }
//        }

        return "formTitle";
    }

    public boolean isFormEncrypted(String formId, String formVersion) {
        boolean encrypted = false;

//        Cursor cursor = getFormsCursor(formId, formVersion);
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    int base64RSAPublicKeyColumnIndex = cursor.getColumnIndex(FormsProviderAPI.FormsColumns.BASE64_RSA_PUBLIC_KEY);
//                    encrypted = cursor.getString(base64RSAPublicKeyColumnIndex) != null;
//                }
//            } finally {
//                cursor.close();
//            }
//        }
        return encrypted;
    }

    public String getFormMediaPath(String formId, String formVersion) {
        String jxQuery = String.format("/.[jrFormId='%s']", formId);
        Form instance = jsonDBTemplate.findOne(jxQuery, Form.class);
        if (instance != null) return instance.getFormMediaPath();
        return null;
    }

    public List<Form> getFormsCursorForFormFilePath(String formFilePath) {
        String jxQuery = String.format("/.[formFilePath='%s']", formFilePath);
        List<Form> instances = jsonDBTemplate.find(jxQuery, Form.class);
        return instances;
    }

    public List<Form> getFormsCursorForMd5Hash(String md5Hash) {
        String jxQuery = String.format("/.[md5Hash='%s']", md5Hash);
        List<Form> instances = jsonDBTemplate.find(jxQuery, Form.class);
        return instances;
    }

    public void deleteFormsDatabase() {
        jsonDBTemplate.dropCollection(Form.class);
    }

    public void deleteFormsFromIDs(ArrayList<String> idsToDelete) {
        for (String idToDelete : idsToDelete) {
            String jxQuery = String.format("/.[id='%s']", idsToDelete);
            Form instance = jsonDBTemplate.findOne(jxQuery, Form.class);
            jsonDBTemplate.remove(instance, Form.class);
        }
    }

    public void deleteFormsFromMd5Hash(String... hashes) {
        ArrayList<String> idsToDelete = new ArrayList<>();
        for (String hash : hashes) {
            List<Form> instances = getFormsCursorForMd5Hash(hash);
            for (Form instance : instances) {
                idsToDelete.add(instance.getId());
            }
        }
        deleteFormsFromIDs(idsToDelete);
    }

    public Form saveForm(Map<String, String> formInfo, File formFile, String mediaPath) {
        Form form = Form.builder()
                .id(UUID.randomUUID().toString())
                .displayName(formInfo.get(FileUtils.TITLE))
                .jrVersion(formInfo.get(FileUtils.VERSION))
                .jrFormId(formInfo.get(FileUtils.FORMID))
                .submissionUri(formInfo.get(FileUtils.SUBMISSIONURI))
                .base64RSAPublicKey(formInfo.get(FileUtils.BASE64_RSA_PUBLIC_KEY))
                .autoDelete(formInfo.get(FileUtils.AUTO_DELETE))
                .autoSend(formInfo.get(FileUtils.AUTO_SEND))
                .geometryXPath(formInfo.get(FileUtils.GEOMETRY_XPATH))
                .formMediaPath(mediaPath)
                .formFilePath(formFile.getAbsolutePath())
                .build();
        jsonDBTemplate.insert(form);
        return form;
    }

    public int updateForm(ContentValues values) {
        return updateForm(values, null, null);
    }

    public int updateForm(ContentValues values, String where, String[] whereArgs) {
        return 0;
    }

    /**
     * Returns all forms available through the cursor and closes the cursor.
     */
    public List<Form> getFormsFromCursor(Cursor cursor) {
        return null;
    }

    public ContentValues getValuesFromFormObject(Form form) {
        return null;
    }
}

