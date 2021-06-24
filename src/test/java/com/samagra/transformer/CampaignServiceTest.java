package com.samagra.transformer;

import com.samagra.transformer.User.CampaignService;
import com.samagra.transformer.User.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CampaignServiceTest {

    @Test
    public void testFormIDForABot() throws Exception {
        String formID = CampaignService.getFirstFormByBotID("fabc64a7-c9b0-4d0b-b8a6-8778757b2bb5");
        Assert.assertEquals("global_form", formID);
    }
}
