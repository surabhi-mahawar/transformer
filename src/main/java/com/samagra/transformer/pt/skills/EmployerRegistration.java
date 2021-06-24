package com.samagra.transformer.pt.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import com.samagra.transformer.samagra.MapEntryConverter;
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
public class EmployerRegistration {
    RestTemplate restTemplate;
    String applicationID;
    String phone;
    Map<String, Object> instanceData;

    public EmployerRegistration updatePhoneNumber(String phone_number){
        updateParams("phone_number", phone_number);
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