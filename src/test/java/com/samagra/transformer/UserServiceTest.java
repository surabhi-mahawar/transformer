package com.samagra.transformer;

import com.samagra.transformer.User.UserService;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class UserServiceTest {

    @Test
    public void getUsersForBot() throws Exception {
        JSONArray ja = UserService.getUsersFromFederatedServers("d89fe2f7-0222-4a38-85fe-37a900468adb");
        assert ja != null;
        assertEquals(35, ja.length());
    }
}
