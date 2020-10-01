package com.samagra.transformer.User;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.UserRequest;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.SearchRequest;
import io.fusionauth.domain.api.user.SearchResponse;
import io.fusionauth.domain.search.UserSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
}
