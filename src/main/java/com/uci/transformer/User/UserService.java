package com.uci.transformer.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.transformer.UserWithTemplate;
import com.uci.utils.CampaignService;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.*;
import io.fusionauth.domain.api.user.SearchRequest;
import io.fusionauth.domain.api.user.SearchResponse;
import io.fusionauth.domain.search.UserSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Service
public class UserService {

    @Autowired
    private CampaignService campaignService;
//    @Autowired
//    @Value("${external.services.url-shortnr.baseURL}")
    private static String shortnrBaseURL = "http://localhost:9999";

    public static User findByEmail(String email) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        ClientResponse<UserResponse, Errors> response = staticClient.retrieveUserByEmail(email);
        if (response.wasSuccessful()) {
            return response.successResponse.user;
        } else if (response.errorResponse != null) {
            Errors errors = response.errorResponse;
        } else if (response.exception != null) {
            // Exception Handling
            Exception exception = response.exception;
        }

        return null;
    }

    public static User findByPhone(String phone) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        UserSearchCriteria usc = new UserSearchCriteria();
        usc.queryString = "*" + phone + "*";
        usc.numberOfResults = 100;
        SearchRequest sr = new SearchRequest(usc);
        ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);
        if (cr.wasSuccessful()) {
            return cr.successResponse.users.get(0);
        } else if (cr.exception != null) {
            // Exception Handling
            Exception exception = cr.exception;
            log.error("Exception in getting users for campaign: " + exception.toString());
        }
        return null;
    }

    public static User findByPhoneAndCampaign(String phone, JsonNode campaign) {
//        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
//        if(campaign != null){
//            UserSearchCriteria usc = new UserSearchCriteria();
//            usc.queryString = "(mobilePhone: " + phone + ") AND (memberships.groupId: " + campaign.data.get("group") +")";
//            usc.numberOfResults = 100;
//            SearchRequest sr = new SearchRequest(usc);
//            ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);
//            if (cr.wasSuccessful()) {
//                return cr.successResponse.users.get(0);
//            } else if (cr.exception != null) {
//                // Exception Handling
//                Exception exception = cr.exception;
//                log.error("Exception in getting users for campaign: " + exception.toString());
//            }
//        }
        return null;
    }

    public static List<User> findUsersForCampaign(String campaignName) throws Exception {

        // Fixme: Important
        /*
        Application currentApplication = CampaignService.getCampaignFromName(campaignName);
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        if(currentApplication != null){
            UserSearchCriteria usc = new UserSearchCriteria();
            usc.numberOfResults = 10000;
            usc.queryString = "(memberships.groupId: " + currentApplication.data.get("group") + ")";
            SearchRequest sr = new SearchRequest(usc);
            ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);

            if (cr.wasSuccessful()) {
                return cr.successResponse.users;
            } else if (cr.exception != null) {
                // Exception Handling
                Exception exception = cr.exception;
                log.error("Exception in getting users for campaign: " + exception.toString());
            }
        }*/
        return new ArrayList<>();
    }

    public static List<User> findUsersForGroup(String group) throws Exception {

        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        UserSearchCriteria usc = new UserSearchCriteria();
        usc.numberOfResults = 10000;
        usc.queryString = "(memberships.groupId: " + group + ")";
        SearchRequest sr = new SearchRequest(usc);
        ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);

        if (cr.wasSuccessful()) {
            return cr.successResponse.users;
        } else if (cr.exception != null) {
            // Exception Handling
            Exception exception = cr.exception;
            log.error("Exception in getting users for campaign: " + exception.toString());
        }
        return new ArrayList<>();
    }


    public List<String> findUsersForESamwad(String campaignName) throws Exception {

        List<String> userPhoneNumbers = new ArrayList<>();

        Set<String> userSet = new HashSet<String>();
        Application currentApplication = campaignService.getCampaignFromNameESamwad(campaignName);
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        FusionAuthClient staticClientLogin = new FusionAuthClient("-vjf6we5HJWexNnOgfWfkuNcYzFx_2Y6WYhSWGj3Frg", "http://www.auth.samagra.io:9011");
        if(currentApplication != null){
            // TODO: Step 1 => Get groups for application
            ArrayList<String> groups = (ArrayList<String>) currentApplication.data.get("group");

            // TODO: Step 3 => eSamwad Login and get token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.loginId = "samarth-admin";
            loginRequest.password = "abcd1234";
            loginRequest.applicationId = UUID.fromString("f0ddb3f6-091b-45e4-8c0f-889f89d4f5da");
            ClientResponse<LoginResponse, Errors> loginResponse = staticClientLogin.login(loginRequest);

            if (loginResponse.wasSuccessful()) {

                String token = loginResponse.successResponse.token;

                // TODO: Step 4 => Iterate over all filters to get phone number data
                for(String group: groups){
                    ClientResponse<GroupResponse, Errors> groupResponse = staticClient.retrieveGroup(UUID.fromString(group));
                    if(groupResponse.wasSuccessful()){
                       String filter = new ObjectMapper().writeValueAsString(groupResponse.successResponse.group.data.get("filterValues"));
                       log.info("Group: " + group + "::" + filter);

                        OkHttpClient client = new OkHttpClient().newBuilder()
                                .build();
                        MediaType mediaType = MediaType.parse("application/json");
                        RequestBody body = RequestBody.create(mediaType, filter);
                        Request request = new Request.Builder()
                                .url("http://esamwad.samagra.io/api/v1/segments/students/")
                                .method("POST", body)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response response = client.newCall(request).execute();
                        String jsonData = response.body().string();
                        JSONObject responseJSON = new JSONObject(jsonData);
                        ArrayList<String> userPhonesResponse = JSONArrayToList((JSONArray) responseJSON.get("data"));

                        // TODO: Step 5 => Create a SET of data to remove duplicates.
                        userSet.addAll(userPhonesResponse);
                    }
                }

                userPhoneNumbers.addAll(userSet);

            } else if (loginResponse.exception != null) {
                // Exception Handling
                Exception exception = loginResponse.exception;
                log.error("Exception in getting users for eSamwad: " + exception.toString());
            }
        }
        return userPhoneNumbers;
    }

    private static ArrayList<String> JSONArrayToList(JSONArray userPhonesResponse) {
        ArrayList<String> usersList = new ArrayList<String>();
        if (userPhonesResponse != null) {
            for (int i=0;i<userPhonesResponse.length();i++){
                usersList.add((String.valueOf(userPhonesResponse.get(i))));
            }
        }
        return usersList;
    }

    public List<String> getUsersPhoneFromFederatedServers(String campaignName){

        Application currentApplication = campaignService.getCampaignFromNameESamwad(campaignName);

        String baseURL = shortnrBaseURL + "/getAllUsers";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        Map data = new HashMap<String, String>();
        data.put("application", currentApplication.id);
        JSONObject jsonData = new JSONObject(data);
        RequestBody body = RequestBody.create(mediaType, jsonData.toString());
        Request request = new Request.Builder()
                .url(baseURL)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            ArrayList<String> userPhonesResponse = JSONArrayToList((new JSONObject(response.body().string())).getJSONArray("data"));
            return userPhonesResponse;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray getUsersFromFederatedServers(String campaignID){

        String baseURL = shortnrBaseURL + "/admin/v1/bot/getAllUsers/" + campaignID;
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(baseURL)
                .addHeader("Content-Type", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            return (new JSONObject(response.body().string())).getJSONArray("data");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<UserWithTemplate> getUsersAndTemplateFromFederatedServers(String campaignName){

        Application currentApplication = campaignService.getCampaignFromNameESamwad(campaignName);
        String baseURL = shortnrBaseURL + "/usersWithTemplate";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        Map data = new HashMap<String, String>();
        data.put("application", currentApplication.id);
        JSONObject jsonData = new JSONObject(data);
        RequestBody body = RequestBody.create(mediaType, jsonData.toString());
        Request request = new Request.Builder()
                .url(baseURL)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            ArrayList<UserWithTemplate> usersWithTemplate = new ArrayList<>();
            JSONArray t = (new JSONObject(response.body().string())).getJSONArray("users");
            for(int i = 0; i < t.length(); i++) {
                JSONObject o = (JSONObject) t.get(i);
                UserWithTemplate e = new UserWithTemplate(o.getString("phone"), o.getString("message"));
                usersWithTemplate.add(e);
            }
            return usersWithTemplate;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
    Get the manager for a specific user
     */
    public static User getManager(User applicant){
        try{
            String managerName = (String) applicant.data.get("reportingManager");
            User u = getUserByFullName(managerName, "SamagraBot");
            if (u != null) return u;
            return null;
        }catch (Exception e){
            return null;
        }
    }

    /*
    Get the programCoordinator for a specific user
     */
    public static User getProgramCoordinator(User applicant){
        try{
            String managerName = (String) applicant.data.get("programCoordinator");
            User u = getUserByFullName(managerName, "SamagraBot");
            if (u != null) return u;
            return null;
        }catch (Exception e){
            return null;
        }
    }

    /*
    Get the programConstruct for a specific user
     */
    public static String getProgramConstruct(User applicant){
        try{
            String programConstruct = String.valueOf(applicant.data.get("programConstruct"));
            if (programConstruct != null) return programConstruct;
            else return "2";
        }catch (Exception e){
            return "2";
        }
    }

    /*
    Get the manager for a specific user
     */
    public static User getEngagementOwner(User applicant){
        try{
            String engagementOwnerName = (String) applicant.data.get("programOwner");
            User u = getUserByFullName(engagementOwnerName, "SamagraBot");
            if (u != null) return u;
            return null;
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public static User getUserByFullName(String fullName, String campaignName) throws Exception {
        List<User> allUsers = findUsersForCampaign(campaignName);
        for (User u : allUsers) {
            if (u.fullName.equals(fullName)) return u;
        }
        return null;
    }


    public static User getInfoForUser(String userID){
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.fromString(userID));
        ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsers(ids);
        return cr.successResponse.users.get(0);
    }

    public static User update(User user){
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        ClientResponse<UserResponse, Errors> userResponse = staticClient.updateUser(user.id, new UserRequest(false, false, user));
        if(userResponse.wasSuccessful()){
            return userResponse.successResponse.user;
        }return null;
    }

    public static Boolean isAssociate(User applicant) {
        try{
            String role = (String) applicant.data.get("role");
            if(role.equals("Program Associate")) return true;
            return false;
        }catch (Exception e){
            return true;
        }
    }

    public static Boolean isCoordinator(User applicant) {
        try{
            String role = (String) applicant.data.get("role");
            if(role.equals("Program Coordinator")) return true;
            return false;
        }catch (Exception e){
            return true;
        }
    }
}
//%E0%A4%A8%E0%A4%AE%E0%A4%B8%E0%A5%8D%E0%A4%95%E0%A4%BE%E0%A4%B0%20Singh%20Reena%20Vijay%20Bahadur%20%E0%A4%9C%E0%A5%80!%0A%0A%E0%A4%AE%E0%A4%BF%E0%A4%B6%E0%A4%A8%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A5%87%E0%A4%B0%E0%A4%A3%E0%A4%BE%20%E0%A4%B8%E0%A5%87%20%E0%A4%9C%E0%A5%81%E0%A5%9C%E0%A4%A8%E0%A5%87%20%E0%A4%95%E0%A5%87%20%E0%A4%B2%E0%A4%BF%E0%A4%8F%20%E0%A4%86%E0%A4%AA%E0%A4%95%E0%A4%BE%20%E0%A4%A7%E0%A4%A8%E0%A5%8D%E0%A4%AF%E0%A4%B5%E0%A4%BE%E0%A4%A6%E0%A5%A4%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%A4%E0%A4%BF%20%E0%A4%AE%E0%A4%BE%E0%A4%B9%20%E0%A4%86%E0%A4%AA%E0%A4%95%E0%A5%87%20%E0%A4%A8%E0%A5%8D%E0%A4%AF%E0%A4%BE%E0%A4%AF%20%E0%A4%AA%E0%A4%82%E0%A4%9A%E0%A4%BE%E0%A4%AF%E0%A4%A4%20%E0%A4%AE%E0%A5%87%E0%A4%82%20%E0%A4%B6%E0%A4%BF%E0%A4%95%E0%A5%8D%E0%A4%B7%E0%A4%95%20%E0%A4%B8%E0%A4%82%E0%A4%95%E0%A5%81%E0%A4%B2%20%E0%A4%A6%E0%A5%8D%E0%A4%B5%E0%A4%BE%E0%A4%B0%E0%A4%BE%20%E0%A4%B8%E0%A4%AD%E0%A5%80%20%E0%A4%B6%E0%A4%BF%E0%A4%95%E0%A5%8D%E0%A4%B7%E0%A4%95%E0%A5%8B%E0%A4%82%20%E0%A4%8F%E0%A4%B5%E0%A4%82%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%A7%E0%A4%BE%E0%A4%A8%E0%A4%BE%E0%A4%A7%E0%A5%8D%E0%A4%AF%E0%A4%BE%E0%A4%AA%E0%A4%95%E0%A5%8B%E0%A4%82%20%E0%A4%95%E0%A5%80%20%E0%A4%8F%E0%A4%95%20%E0%A4%AC%E0%A5%88%E0%A4%A0%E0%A4%95/%E0%A4%95%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%AF%E0%A4%B6%E0%A4%BE%E0%A4%B2%E0%A4%BE%20%E0%A4%86%E0%A4%AF%E0%A5%8B%E0%A4%9C%E0%A4%BF%E0%A4%A4%20%E0%A4%95%E0%A5%80%20%E0%A4%9C%E0%A4%BE%E0%A4%A8%E0%A5%80%20%E0%A4%B9%E0%A5%88%20I%20%0A%0A%E0%A4%87%E0%A4%B8%20%E0%A4%B5%E0%A5%8D%E0%A4%B9%E0%A4%BE%E0%A4%9F%E0%A5%8D%E0%A4%B8%E0%A4%AA%E0%A5%8D%E0%A4%AA%20%E0%A4%A8%E0%A4%82%E0%A4%AC%E0%A4%B0%20%E0%A4%95%E0%A5%87%20%E0%A4%AE%E0%A4%BE%E0%A4%A7%E0%A5%8D%E0%A4%AF%E0%A4%AE%20%E0%A4%B8%E0%A5%87%20%E0%A4%87%E0%A4%B8%20%E0%A4%AC%E0%A5%88%E0%A4%A0%E0%A4%95%20%E0%A4%AE%E0%A5%87%E0%A4%82%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%A4%E0%A4%BF%E0%A4%AD%E0%A4%BE%E0%A4%97%20%E0%A4%95%E0%A4%B0%E0%A4%A8%E0%A5%87%20%E0%A4%95%E0%A5%87%20%E0%A4%AC%E0%A4%BE%E0%A4%A6%20%E0%A4%95%E0%A5%81%E0%A4%9B%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%B6%E0%A5%8D%E0%A4%A8%E0%A5%8B%E0%A4%82%20%E0%A4%95%E0%A5%87%20%E0%A4%AE%E0%A4%BE%E0%A4%A7%E0%A5%8D%E0%A4%AF%E0%A4%AE%20%E0%A4%B8%E0%A5%87%20%E0%A4%85%E0%A4%AA%E0%A4%A8%E0%A4%BE%20feedback%20%E0%A4%A6%E0%A5%87%E0%A4%82%20I%20*Feedback%20%E0%A4%AE%E0%A5%87%E0%A4%82%20%E0%A4%86%E0%A4%AA%E0%A4%95%E0%A5%8B%205-7%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%B6%E0%A5%8D%E0%A4%A8%E0%A5%8B%E0%A4%82%20%E0%A4%95%E0%A4%BE%20%E0%A4%89%E0%A4%A4%E0%A5%8D%E0%A4%A4%E0%A4%B0%20%E0%A4%A6%E0%A5%87%E0%A4%A8%E0%A4%BE%20%E0%A4%B9%E0%A5%8B%E0%A4%97%E0%A4%BE,%20%E0%A4%9C%E0%A4%BF%E0%A4%B8%E0%A4%AE%E0%A5%87%E0%A4%82%20%E0%A4%B8%E0%A4%BF%E0%A4%B0%E0%A5%8D%E0%A4%AB%202%20%E0%A4%AE%E0%A4%BF%E0%A4%A8%E0%A4%9F%20%E0%A4%B2%E0%A4%97%E0%A5%87%E0%A4%82%E0%A4%97%E0%A5%87I*%0A%0AFeedback%20%E0%A4%AD%E0%A4%B0%E0%A4%A8%E0%A5%87%20%E0%A4%95%E0%A5%87%20%E0%A4%B2%E0%A4%BF%E0%A4%8F%20%E0%A4%A8%E0%A5%80%E0%A4%9A%E0%A5%87%20%E0%A4%A6%E0%A4%BF%E0%A4%8F%20%E0%A4%97%E0%A4%8F%20%E0%A4%AC%E0%A4%9F%E0%A4%A8%20%E2%80%98*%E0%A4%A8%E0%A4%AE%E0%A4%B8%E0%A5%8D%E0%A4%95%E0%A4%BE%E0%A4%B0%20%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A5%87%E0%A4%B0%E0%A4%A3%E0%A4%BE%20%E0%A4%AC%E0%A5%89%E0%A4%9F*%E2%80%99%20%E0%A4%AA%E0%A4%B0%20%E0%A4%95%E0%A5%8D%E0%A4%B2%E0%A4%BF%E0%A4%95%20%E0%A4%95%E0%A4%B0%E0%A5%87%E0%A4%82%20I%0A%0A%E0%A4%A7%E0%A4%A8%E0%A5%8D%E0%A4%AF%E0%A4%B5%E0%A4%BE%E0%A4%A6!%20%0A%E0%A4%AC%E0%A5%87%E0%A4%B8%E0%A4%BF%E0%A4%95%20%E0%A4%B6%E0%A4%BF%E0%A4%95%E0%A5%8D%E0%A4%B7%E0%A4%BE%20%E0%A4%B5%E0%A4%BF%E0%A4%AD%E0%A4%BE%E0%A4%97
