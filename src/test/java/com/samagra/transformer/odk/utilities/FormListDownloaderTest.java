package com.samagra.transformer.odk.utilities;

import android.webkit.MimeTypeMap;
import com.samagra.transformer.odk.FormDownloader;
import com.samagra.transformer.odk.model.FormDetails;
import com.samagra.transformer.odk.openrosa.CollectThenSystemContentTypeMapper;
import com.samagra.transformer.odk.openrosa.OpenRosaAPIClient;
import com.samagra.transformer.odk.openrosa.OpenRosaHttpInterface;
import com.samagra.transformer.odk.openrosa.okhttp.OkHttpConnection;
import com.samagra.transformer.odk.openrosa.okhttp.OkHttpOpenRosaServerClientProvider;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=FormListDownloaderTest.class)
@EnableConfigurationProperties
public class FormListDownloaderTest {
//
//    @Test
//    public void formListDownloadTest() {
//        OpenRosaHttpInterface openRosaHttpInterface = new OkHttpConnection(
//                new OkHttpOpenRosaServerClientProvider(new OkHttpClient()),
//                null,
//                "userAgent"
//        );
//        WebCredentialsUtils webCredentialsUtils = new WebCredentialsUtils();
//        OpenRosaAPIClient openRosaAPIClient = new OpenRosaAPIClient(openRosaHttpInterface, webCredentialsUtils);
//        FormListDownloader formListDownloader = new FormListDownloader(
//                openRosaAPIClient,
//                webCredentialsUtils);
//        HashMap<String, FormDetails> formList = formListDownloader.downloadFormList(false);
//        System.out.println(formList);
//        int count = 0;
//        if(formList.size() > 0) {
//            ArrayList<FormDetails> forms = new ArrayList<>();
//            for (Map.Entry<String, FormDetails> form : formList.entrySet()) {
//                if(count ==2) {
//                    forms.add(form.getValue());
//                }
//                count+=1;
//            }
//            FormDownloader formDownloader = new FormDownloader(openRosaAPIClient);
//            formDownloader.downloadForms(forms);
//
//        }
//    }

}