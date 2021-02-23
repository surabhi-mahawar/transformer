package com.samagra.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.samagra.MapEntryConverter;
import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;

public class eSamwad {

    @Test
    public void testUsersForESamwad() throws Exception {
        List<String> users = UserService.findUsersForESamwad("Test 4/11");
        System.out.println(users.size());
        Assert.assertTrue(users.size() > 52000);
    }

}
