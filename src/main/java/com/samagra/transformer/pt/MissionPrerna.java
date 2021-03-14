package com.samagra.transformer.pt;

import com.samagra.transformer.samagra.MapEntryConverter;
import com.thoughtworks.xstream.XStream;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Builder
@Getter
@Setter
public class MissionPrerna {
    RestTemplate restTemplate;
    String applicationID;
    String phone;
    Map<String, Object> instanceData;

    public static String shortnrBaseURL = "https://url.samagra.io";

    public String getInitialValue() {
        UUID instanceID = randomUUID();

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        Map data = new HashMap<String, String>();
        data.put("applicationID", this.applicationID);
        data.put("phone", this.phone);
        JSONObject jsonData = new JSONObject(data);
        RequestBody body = RequestBody.create(mediaType, jsonData.toString());
        Request request = new Request.Builder()
                .url(shortnrBaseURL + "/user/eval")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        String d = "";

        try {
            Response response = client.newCall(request).execute();
            JSONArray jsonArray = (new JSONObject(response.body().string())).getJSONArray("data");
            HashMap<String, String> fields = new HashMap<>();
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject object = (JSONObject) jsonArray.get(i);
                fields.put(object.getString("field"), object.getString("evaluated"));
                d = updateParams(object.getString("field"), object.getString("evaluated"));
            }
            fields.put("uuid", instanceID.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return d;
    }

    public String updateParams(String key, String value) {
        ((Map<String, Object>) this.instanceData.get("application_process")).put(key, value);
        XStream magicApi = new XStream();
        magicApi.registerConverter(new MapEntryConverter());
        magicApi.alias("data", Map.class);
        return magicApi.toXML(this.instanceData);
    }

    public Map<String, Object> parse(String xml) {
        XStream magicApi = new XStream();
        magicApi.registerConverter(new MapEntryConverter());
        magicApi.alias("data", Map.class);
        this.instanceData = (Map<String, Object>) magicApi.fromXML(xml);
        return this.instanceData;
    }

}