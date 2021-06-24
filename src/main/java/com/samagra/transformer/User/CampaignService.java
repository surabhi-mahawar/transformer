package com.samagra.transformer.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.api.ApplicationResponse;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class CampaignService {

    @Value("${campaign.url}")
    public String CAMPAIGN_URL;

    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public JsonNode getCampaignFromID(String campaignID) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String baseURL = CAMPAIGN_URL+ "admin/v1/bot/get/" + campaignID;
            ResponseEntity<String> response = restTemplate.getForEntity(baseURL, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(response.getBody());
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public JsonNode getCampaignFromName(String campaignName) {
        RestTemplate restTemplate = new RestTemplate();
        String url = CAMPAIGN_URL + "admin/v1/bot/search/?name=" + campaignName + "&match=true";
        ResponseEntity<String> response
                = restTemplate.getForEntity(url, String.class);
        if(response.getStatusCode() == HttpStatus.OK){
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(response.getBody()).get("data").get(0);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param botID - Bot ID
     * @return FormID for the first transformer.
     * @throws Exception Error Exception, in failure in Network request.
     */
    public String getFirstFormByBotID(String botID) {
        RestTemplate restTemplate = new RestTemplate();
        String baseURL = CAMPAIGN_URL + "admin/v1/bot/get/";
        ResponseEntity<String> response = restTemplate.getForEntity(baseURL + botID, String.class);
        if(response.getStatusCode() == HttpStatus.OK){
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(response.getBody()).findValue("formID").asText();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param botID - Bot ID
     * @return FormID for the first transformer.
     * @throws Exception Error Exception, in failure in Network request.
     */
    public ArrayNode getFirstFormHiddenFields(String botID) {
        RestTemplate restTemplate = new RestTemplate();
        String baseURL = CAMPAIGN_URL + "/admin/v1/bot/get/";
        ResponseEntity<String> response = restTemplate.getForEntity(baseURL + botID, String.class);
        if(response.getStatusCode() == HttpStatus.OK){
            ObjectMapper mapper = new ObjectMapper();
            try {
                return (ArrayNode) mapper.readTree(response.getBody()).findValue("hiddenFields");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromNameESamwad(String campaignName) {
        List<Application> applications = new ArrayList<>();
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        ClientResponse<ApplicationResponse, Void> response = staticClient.retrieveApplications();
        if (response.wasSuccessful()) {
            applications = response.successResponse.applications;
        } else if (response.exception != null) {
            Exception exception = response.exception;
        }

        Application currentApplication = null;
        if (applications.size() > 0) {
            for (Application application : applications) {
                try {
                    if (application.data.get("appName").equals(campaignName)) {
                        currentApplication = application;
                    }
                }catch (Exception e){

                }
            }
        }
        return currentApplication;
    }
}
