package com.samagra.transformer.odk.persistance;

import android.content.ContentValues;
import android.database.Cursor;

import com.samagra.transformer.odk.model.Form;
import com.samagra.transformer.odk.utilities.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FormsDao {

    public Cursor getFormsCursor() {
        return getFormsCursor(null, null, null, null);
    }

    public Cursor getFormsCursor(String selection, String[] selectionArgs) {
        return getFormsCursor(null, selection, selectionArgs, null);
    }

    Cursor getFormsCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
        //        return Collect.getInstance().getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    public Cursor getFormsCursor(String formId, String formVersion) {
        return null;
//        String[] selectionArgs;
//        String selection;
//
//        if (formVersion == null) {
//            selectionArgs = new String[]{formId};
//            selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? AND "
//                    + FormsProviderAPI.FormsColumns.JR_VERSION + " IS NULL";
//        } else {
//            selectionArgs = new String[]{formId, formVersion};
//            selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? AND "
//                    + FormsProviderAPI.FormsColumns.JR_VERSION + "=?";
//        }
//
//        // As long as we allow storing multiple forms with the same id and version number, choose
//        // the newest one
//        String order = FormsProviderAPI.FormsColumns.DATE + " DESC";
//
//        return getFormsCursor(null, selection, selectionArgs, order);
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
        String formMediaPath = null;

//        Cursor cursor = getFormsCursor(formId, formVersion);
//
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    int formMediaPathColumnIndex = cursor.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_MEDIA_PATH);
//                    formMediaPath = cursor.getString(formMediaPathColumnIndex);
//                }
//            } finally {
//                cursor.close();
//            }
//        }
        return formMediaPath;
    }

    public List<Form> getFormsCursorForFormFilePath(String formFilePath) {
        Form form = new Form();
        form.setId();
        form.setHostname("ec2-54-191-11");
        form.setPrivateKey("b87eb02f5dd7e5232d7b0fc30a5015e4");
        jsonDBTemplate.insert(instance);
        return null;

//        return getFormsCursor(null, selection, selectionArgs, null);
    }

    public List<Form> getFormsCursorForMd5Hash(String md5Hash) {

        String jxQuery = String.format("/.[md5Hash='%s']", md5Hash);
        List<Form> instances = jsonDBTemplate.find(jxQuery, Form.class);
        return instances;
    }

    public void deleteFormsDatabase() {
    }

    public void deleteFormsFromIDs(String[] idsToDelete) {
    }

    public void deleteFormsFromMd5Hash(String... hashes) {
//        List<String> idsToDelete = new ArrayList<>();
//        Cursor c = null;
//        try {
//            for (String hash : hashes) {
//                c = getFormsCursorForMd5Hash(hash);
//                if (c != null && c.moveToFirst()) {
//                    String id = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns._ID));
//                    idsToDelete.add(id);
//                    c.close();
//                    c = null;
//                }
//            }
//        } finally {
//            if (c != null) {
//                c.close();
//            }
//        }
//        deleteFormsFromIDs(idsToDelete.toArray(new String[idsToDelete.size()]));
    }

    public Form saveForm(Map<String, String> formInfo, File formFile, String mediaPath) {
        Form form = Form.builder().id(UUID.randomUUID().toString())
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

