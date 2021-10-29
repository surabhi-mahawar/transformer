package com.uci.transformer.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.transformer.odk.entity.Assessment;
import com.uci.transformer.odk.entity.Question;
import com.uci.utils.telemetry.dto.*;
import com.uci.utils.telemetry.util.TelemetryEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssessmentTelemetryBuilder {


    private static final String TELEMETRY_IMPL_VERSION = "3.0";
    private static final String QUESTION_TELEMETRY_IMPL_VERSION = "1.0";
    private static final String ACTOR_TYPE_USER = "User";
    private static final String DIKSHA_ORG = "DIKSHA";
    private static final String ASSESS_EVENT_MID = "fb3db9abceb578d8acbc812cdbd9c931";
    //represents the MD5 Hash of "Individual Question-Responses - Survey/Questionnaire"

    public String build(
            String botOrg,
            String channel,
            String provider,
            String producerID,
            String conversationOwnerID,
            Question question,
            Assessment assessment,
            long duration) {
        List<Map<String, Object>> cdata = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("ConversationOwner", conversationOwnerID);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("Conversation", assessment.getBotID().toString());
        cdata.add(map1);
        cdata.add(map2);
        Map<String, String> rollup = new HashMap<>();
        rollup.put("l1", "ConversationOwner");
        rollup.put("l2", "Conversation");
        String channelName = (botOrg.equalsIgnoreCase("Anonymous")) ? DIKSHA_ORG : botOrg;
        String userID = "";
        try{
            userID = assessment.getUserID().toString();
        }catch (Exception e){}
        Context context = Context.builder()
                .channel(channelName)
                .env(channel + "." + provider)
                .pdata(Producer.builder()
                        .id("prod.uci.diksha")
                        .pid(producerID).ver(QUESTION_TELEMETRY_IMPL_VERSION)
                        .build()
                )
                .did(userID)
                .cdata(cdata)
                .rollup(rollup)
                .build();

        Map<String, String> questionRollup = new HashMap<>();
        questionRollup.put("l1", "BotOwnerID");
        questionRollup.put("l2", "BotID");
        questionRollup.put("l3", "QuestionID");
        Target object = Target.builder().id(question.getId().toString())
                .type(question.getQuestionType().name()).ver(QUESTION_TELEMETRY_IMPL_VERSION)
                .rollup(questionRollup).build();

        Map<String, Object> edata = new HashMap<>();
        ArrayList<HashMap<String, String>> resValues = new ArrayList<>();
        HashMap<String, String> value1 = new HashMap<>();
        value1.put("ans1", assessment.getAnswer());
        resValues.add(value1);
        edata.put("duration", duration);
        Map<String, Object> itemDetails = new HashMap<>();
        itemDetails.put("botID", assessment.getBotID().toString());
        itemDetails.put("userID", userID.toString());
        itemDetails.put("id", question.getId().toString());
        itemDetails.put("type", question.getQuestionType().name());
        itemDetails.put("meta", question.getMeta());
        edata.put("item", itemDetails);
        edata.put("resValues", resValues);
        Telemetry telemetry = Telemetry.builder().
                eid(TelemetryEvents.ASSESS.getName()).
                ets(System.currentTimeMillis()).
                ver(TELEMETRY_IMPL_VERSION).
                mid(ASSESS_EVENT_MID)
                .actor(Actor.builder()
                        .type(ACTOR_TYPE_USER)
                        .id(userID)
                        .build())
                .context(context)
                .object(object)
                .edata(edata)
                .build();
        return Telemetry.getTelemetryRequestData(telemetry);
    }


}
