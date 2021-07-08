package com.uci.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.uci.transformer.TransformerProvider;
import com.uci.transformer.User.UserService;
import com.uci.transformer.odk.entity.Assessment;
import com.uci.transformer.odk.entity.GupshupMessageEntity;
import com.uci.transformer.odk.entity.GupshupStateEntity;
import com.uci.transformer.odk.entity.Question;
import com.uci.transformer.odk.persistance.FormsDao;
import com.uci.transformer.odk.persistance.JsonDB;
import com.uci.transformer.odk.repository.AssessmentRepository;
import com.uci.transformer.odk.repository.MessageRepository;
import com.uci.transformer.odk.repository.QuestionRepository;
import com.uci.transformer.odk.repository.StateRepository;
import com.uci.transformer.odk.utilities.FormUpdation;
import com.uci.transformer.pt.skills.EmployerRegistration;
import com.uci.transformer.samagra.SamagraOrgForm;
import com.uci.transformer.samagra.TemplateServiceUtils;
import com.uci.utils.CampaignService;
import com.uci.utils.CommonProducer;
import io.fusionauth.domain.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.xml.XMessageParser;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static messagerosa.core.model.XMessage.MessageState.NOT_SENT;
import static messagerosa.core.model.XMessage.MessageType.HSM;

@Slf4j
@Component
public class ODKTransformer extends TransformerProvider {

    private static final String SMS_BROADCAST_IDENTIFIER = "Broadcast";
    public static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    QuestionRepository questionRepo;

    @Autowired
    AssessmentRepository assessmentRepo;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private MessageRepository msgRepo;

    @Qualifier("custom")
    @Autowired
    private RestTemplate customRestTemplate;

    @Qualifier("rest")
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    DataSource dataSource;

    @Autowired
    CampaignService campaignService;


    // Listen to all ODK based transformers
    @KafkaListener(id = "odk-transformer", topicPattern = "com.odk.*")
    public void consumeMessage(String message) throws Exception {
        long startTime = System.nanoTime();
        log.info("Form Transformer Message: " + message);

        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        if (xMessage.getMessageType() == XMessage.MessageType.BROADCAST_TEXT) {
//            ArrayList<XMessage> messages = (ArrayList<XMessage>) this.


            this.transformToMany(xMessage).subscribe(new Consumer<List<XMessage>>() {

                @Override
                public void accept(List<XMessage> messages) {
                    messages = (ArrayList<XMessage>) messages;
                    for (XMessage msg : messages) {

                        try {
                            kafkaProducer.send("outbound", msg.toXML());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        } catch (JAXBException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        } else {
            this.transform(xMessage).subscribe(new Consumer<XMessage>() {
                @Override
                public void accept(XMessage transformedMessage) {
                    if (transformedMessage != null) {
                        try {
                            kafkaProducer.send("outbound", transformedMessage.toXML());
                            long endTime = System.nanoTime();
                            long duration = (endTime - startTime);
                            log.error("Total time spent in processing form: " + duration / 1000000);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        } catch (JAXBException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

        }
    }

    private FormManagerParams getPreviousMetadata(XMessage message, String formID) {
        String prevPath = null;
        String prevXMl = null;
        FormManagerParams formManagerParams = new FormManagerParams();

        if (!message.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            GupshupStateEntity stateEntity = stateRepo.findByPhoneNoAndBotFormName(message.getTo().getUserID(), formID);

            if (stateEntity != null && message.getPayload() != null) {
                prevXMl = stateEntity.getXmlPrevious();
                prevPath = stateEntity.getPreviousPath();
            }

            // Handle image responses to a question
            if (message.getPayload() != null) {
                if (message.getPayload().getText() == null) {
                    formManagerParams.setCurrentAnswer(message.getPayload().getMedia().getUrl());
                } else formManagerParams.setCurrentAnswer(message.getPayload().getText());
            } else {
                formManagerParams.setCurrentAnswer("");
            }
        } else {
            formManagerParams.setCurrentAnswer("");
        }

        formManagerParams.setPreviousPath(prevPath);
        formManagerParams.setInstanceXMlPrevious(prevXMl);
        return formManagerParams;
    }


    @Override
    public Mono<XMessage> transform(XMessage xMessage) throws Exception {
        return campaignService.getCampaignFromNameTransformer(xMessage.getApp()).flatMap(new Function<JsonNode, Mono<? extends XMessage>>() {
            @Override
            public Mono<XMessage> apply(JsonNode campaign) {
                if (campaign != null) {
                    String formID = getFormID(campaign);
                    if (formID.equals("")) {
                        log.error("Unable to find form ID from Conversation Logic");
                        return Mono.just(null);
                    }
                    String formPath = getFormPath(formID);
                    boolean isStartingMessage = xMessage.getPayload().getText().equals(campaign.findValue("startingMessage").asText());
                    switchFromTo(xMessage);

                    // Get details of user from database
                    FormManagerParams previousMeta = getPreviousMetadata(xMessage, formID);

                    final ServiceResponse[] response = new ServiceResponse[1];
                    MenuManager mm;
                    if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*") || isStartingMessage) {
                        previousMeta.currentAnswer = "*";
                        ServiceResponse serviceResponse = new MenuManager(null, null, null, formPath, formID,false,questionRepo).start();
                        FormUpdation ss = FormUpdation.builder().build();
                        ss.parse(serviceResponse.currentResponseState);
                        ss.updateAdapterProperties(xMessage.getChannel(), xMessage.getProvider());
                        String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                ss.getXML();
                        log.debug("Instance value >> "+ instanceXMlPrevious);
                        mm = new MenuManager(null, null, instanceXMlPrevious, formPath, formID,true,questionRepo);
                        response[0]=mm.start();
                    } else {
                        mm = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer,
                                previousMeta.instanceXMlPrevious, formPath, formID, false, questionRepo);
                        response[0] = mm.start();
                    }

                    // Save answerData => PreviousQuestion + CurrentAnswer
                    List<Question> questionList = questionRepo.findQuestionByXPathAndFormIDAndFormVersion(previousMeta.previousPath,
                            formID, response[0].formVersion);
                    if (questionList != null && questionList.size() > 0) {
                        Assessment assessment = Assessment.builder()
                                .question(questionList.get(0))
                                        .answer(previousMeta.currentAnswer)
                                .botID(UUID.fromString(campaign.findValue("id").asText()))
                                .build();

                        assessmentRepo.save(assessment);
                    }else{
                        log.error("No question asked previously, error of DB misconfiguration. Please delete your questions");
                    }

                    if (mm.isGlobal() && response[0].currentIndex.contains("eof__")) {
                        String nextBotID = mm.getNextBotID(response[0].currentIndex);
                        return campaignService
                                .getFirstFormByBotID(nextBotID)
                                .map(new Function<String, XMessage>() {
                                         @Override
                                         public XMessage apply(String nextFormID) {
                                             MenuManager mm2 = new MenuManager(null,
                                                     null, null, getFormPath(nextFormID),
                                                     nextFormID, false, questionRepo);
                                             response[0] = mm2.start();
                                             return decodeXMessage(xMessage, response[0], formID);
                                         }
                                     }
                                );

                    } else {
                        return Mono.just(decodeXMessage(xMessage, response[0], formID));
                    }
                }else{
                    log.error("Could not find Bot");
                    return Mono.just(null);
                }
            }
        }).doOnError(throwable -> {
            log.error("Error in api" + throwable.getMessage());
        });

    }

    private XMessage decodeXMessage(XMessage xMessage, ServiceResponse response, String formID) {
        XMessage nextMessage = getMessageFromResponse(xMessage, response);

        // Update database with new fields.
        appendNewResponse(formID, xMessage, response);
        replaceUserState(formID, xMessage, response);

        if (isEndOfForm(response)) {
            new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate);
        }

        return getClone(nextMessage);
    }

    private boolean isEndOfForm(ServiceResponse response) {
        return response.getCurrentIndex().equals("endOfForm") || response.currentIndex.contains("eof");
    }

    private String getFormID(JsonNode campaign) {
        try {
            return campaign.findValue("formID").asText();
        } catch (Exception e) {
            return "";
        }
    }

    @Nullable
    private XMessage getClone(XMessage nextMessage) {
        XMessage cloneMessage = null;
        try {
            cloneMessage = XMessageParser.parse(new ByteArrayInputStream(nextMessage.toXML().getBytes()));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return cloneMessage;
    }

    private void switchFromTo(XMessage xMessage) {
        SenderReceiverInfo from = xMessage.getFrom();
        SenderReceiverInfo to = xMessage.getTo();
        xMessage.setFrom(to);
        xMessage.setTo(from);
    }


    @Override
    public Mono<List<XMessage>> transformToMany(XMessage xMessage) {

        ArrayList<XMessage> messages = new ArrayList<>();

        // Get All Users with Data.
        return campaignService.getCampaignFromNameTransformer(xMessage.getCampaign()).map(new Function<JsonNode, List<XMessage>>() {
            @Override
            public List<XMessage> apply(JsonNode campaign) {
                String campaignID = campaign.get("id").asText();
                JSONArray users = UserService.getUsersFromFederatedServers(campaignID);
                String formID = getFormID(campaign);
                String formPath = getFormPath(formID);
                JsonNode firstTransformer = campaign.findValues("transformers").get(0).get(0);
                ArrayNode hiddenFields = (ArrayNode) firstTransformer.findValue("hiddenFields");

                for (int i = 34; i < users.length(); i++) {
                    String userPhone = ((JSONObject) users.get(i)).getString("whatsapp_mobile_number");
                    ServiceResponse response = new MenuManager(null, null, null, formPath, formID, false, questionRepo).start();
                    FormUpdation ss = FormUpdation.builder().applicationID(campaignID).phone(userPhone).build();
                    ss.updateAdapterProperties(xMessage.getChannel(), xMessage.getProvider());
                    ss.parse(response.currentResponseState);
                    String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ss.updateHiddenFields(hiddenFields, (JSONObject) users.get(i)).getXML();
                    MenuManager mm = new MenuManager(null, null, instanceXMlPrevious, formPath, formID, true, questionRepo);
                    response = mm.start();

                    // Create new xMessage from response
                    XMessage x = getMessageFromResponse(xMessage, response);
                    XMessage nextMessage = getClone(x);

                    // Update user info
                    SenderReceiverInfo to = nextMessage.getTo();
                    to.setUserID(userPhone);
                    nextMessage.setTo(to);

                    nextMessage.setMessageState(NOT_SENT);
                    nextMessage.setMessageType(HSM);

                    // Update database with new fields.
                    appendNewResponse(formID, nextMessage, response);
                    replaceUserState(formID, nextMessage, response);
                    messages.add(nextMessage);
                }
                return messages;
            }
        });

    }

    private XMessage getMessageFromResponse(XMessage xMessage, ServiceResponse response) {
        // Add payload to the response
        XMessagePayload payload = response.getNextMessage();
        xMessage.setPayload(payload);
        return xMessage;
    }


    public static String getFormPath(String formID) {
        FormsDao dao = new FormsDao(JsonDB.getInstance().getDB());
        return dao.getFormsCursorForFormId(formID).getFormFilePath();
    }

    private void appendNewResponse(String formID, XMessage xMessage, ServiceResponse response) {
        GupshupMessageEntity msgEntity = new GupshupMessageEntity();
        msgEntity.setPhoneNo(xMessage.getTo().getUserID());
        msgEntity.setMessage(xMessage.getPayload().getText());
        msgEntity.setLastResponse(response.getCurrentIndex().equals("endOfForm"));
        msgRepo.save(msgEntity);
    }

    private void replaceUserState(String formID, XMessage xMessage, ServiceResponse response) {
        GupshupStateEntity saveEntity = stateRepo.findByPhoneNoAndBotFormName(xMessage.getTo().getUserID(), formID);
        if (saveEntity == null) {
            saveEntity = new GupshupStateEntity();
        }
        saveEntity.setPhoneNo(xMessage.getTo().getUserID());
        saveEntity.setPreviousPath(response.getCurrentIndex());
        saveEntity.setXmlPrevious(response.getCurrentResponseState());
        saveEntity.setBotFormName(formID);
        stateRepo.save(saveEntity);
    }
}