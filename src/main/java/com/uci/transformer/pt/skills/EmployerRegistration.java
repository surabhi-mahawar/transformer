package com.uci.transformer.pt.skills;

import com.uci.transformer.samagra.MapEntryConverter;
import com.thoughtworks.xstream.XStream;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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