package com.uci.transformer.pt.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import com.uci.transformer.samagra.MapEntryConverter;
import com.thoughtworks.xstream.XStream;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Builder
@Getter
@Setter
public class CandidateApproval {
    RestTemplate restTemplate;
    String applicationID;
    String phone;
    Map<String, Object> instanceData;

    public CandidateApproval updateAdapterProperties(String channel, String provider){
        updateParams("channel", channel);
        updateParams("provider", provider);
        return this;
    }

    public CandidateApproval updateHiddenFields(ArrayNode hiddenFields, JSONObject user) {
        UUID instanceID = randomUUID();
        HashMap<String, String> fields = new HashMap<>();
        for(int i=0; i<hiddenFields.size(); i++){
            JsonNode object = hiddenFields.get(i);
            String userField = JsonPath.parse(user.toString()).read("$." + object.findValue("path").asText(), String.class);
            updateParams(object.findValue("name").asText(), userField);
        }
        fields.put("uuid", instanceID.toString());
        return this;
    }

    public void updateParams(String key, String value) {
        ((Map<String, Object>) this.instanceData.get("intro_group")).put(key, value);
    }

    public String getXML(){
        XStream magicApi = new XStream();
        magicApi.registerConverter(new MapEntryConverter());
        magicApi.alias("data", Map.class);
        return magicApi.toXML(this.instanceData).replaceAll("__", "_");
    }

    public Map<String, Object> parse(String xml) {
        XStream magicApi = new XStream();
        magicApi.registerConverter(new MapEntryConverter());
        magicApi.alias("data", Map.class);
        this.instanceData = (Map<String, Object>) magicApi.fromXML(xml);
        return this.instanceData;
    }

}