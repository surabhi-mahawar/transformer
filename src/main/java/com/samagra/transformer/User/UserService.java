package com.samagra.transformer.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class UserService {

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

    public static User findByPhoneAndCampaign(String phone, Application campaign) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        if(campaign != null){
            UserSearchCriteria usc = new UserSearchCriteria();
            usc.queryString = "(mobilePhone: " + phone + ") AND (memberships.groupId: " + campaign.data.get("group") +")";
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
        }
        return null;
    }

    public static List<User> findUsersForCampaign(String campaignName) throws Exception {

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
        }
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


    public static List<String> findUsersForESamwad(String campaignName) throws Exception {

        List<String> userPhoneNumbers = new ArrayList<>();

        Set<String> userSet = new HashSet<String>();
        Application currentApplication = CampaignService.getCampaignFromNameESamwad(campaignName);
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
