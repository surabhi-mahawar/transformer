package com.samagra.transformer.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.api.ApplicationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class CampaignService {

    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromID(String campaignID) throws Exception {
        System.out.println("CampaignID: " + campaignID);
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        System.out.println("Client: " + staticClient);
        ClientResponse<ApplicationResponse, Void> applicationResponse = staticClient.retrieveApplication(UUID.fromString(campaignID));
        if (applicationResponse.wasSuccessful()) {
            Application application = applicationResponse.successResponse.application;
            Map<String, Object> campaignData = new HashMap<>();
            ArrayList<String> transformers = new ArrayList<>();
            // transformers.add(0, "Broadcast::SMS_1"); //SMS_1 refers to the template ID.
            // transformers.add(1, "FORM::FORM_ID_1"); //Form_ID_1 refers to first step ODK Form
            transformers.add(0, "FORM::RQ"); //Form_ID_2 refers to second step ODK Form
            //If it contains only Broadcast ---> SMS based campaign
            //If only FORM::Form_ID --> ODK Based campaign
            campaignData.put("transformers", transformers);
            application.data = campaignData;
            return application;
        } else if (applicationResponse.exception != null) {
            Exception exception = applicationResponse.exception;
            throw exception;
        }
        return null;
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static JsonNode getCampaignFromName(String campaignName) {
        RestTemplate restTemplate = new RestTemplate();
        String baseURL = "http://federation-service:9999/admin/v1/bot/get/?name=";
        ResponseEntity<String> response
                = restTemplate.getForEntity(baseURL + campaignName, String.class);
        if(response.getStatusCode() == HttpStatus.OK){
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(response.getBody());
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
