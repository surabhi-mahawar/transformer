package com.uci.transformer.odk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Maps;
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
import com.uci.transformer.telemetry.AssessmentTelemetryBuilder;
import com.uci.utils.CampaignService;
import com.uci.utils.kafka.SimpleProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.xml.XMessageParser;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.function.Tuple2;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static messagerosa.core.model.XMessage.MessageState.NOT_SENT;
import static messagerosa.core.model.XMessage.MessageType.HSM;

@Component
@RequiredArgsConstructor
@Slf4j
public class ODKConsumerReactive extends TransformerProvider {

    private final Flux<ReceiverRecord<String, String>> reactiveKafkaReceiver;

    private static final String SMS_BROADCAST_IDENTIFIER = "Broadcast";
    public static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @Value("${outbound}")
    public String outboundTopic;

    @Value("${telemetry}")
    public String telemetryTopic;

    @Autowired
    public SimpleProducer kafkaProducer;

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
    CampaignService campaignService;

    @Value("${producer.id}")
    private String producerID;

    @EventListener(ApplicationStartedEvent.class)
    public void onMessage() {
        reactiveKafkaReceiver
                .doOnNext(new Consumer<ReceiverRecord<String, String>>() {
                    @Override
                    public void accept(ReceiverRecord<String, String> stringMessage) {
                        final long startTime = System.nanoTime();
                        try {
                            XMessage msg = XMessageParser.parse(new ByteArrayInputStream(stringMessage.value().getBytes()));
                            logTimeTaken(startTime, 1);
                            if (msg.getMessageType() == XMessage.MessageType.BROADCAST_TEXT) {
                                transformToMany(msg).subscribe(new Consumer<List<XMessage>>() {
                                    @Override
                                    public void accept(List<XMessage> messages) {
                                        messages = (ArrayList<XMessage>) messages;
                                        for (XMessage msg : messages) {
                                            try {
                                                kafkaProducer.send(outboundTopic, msg.toXML());
                                            } catch (JAXBException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            } else {
                                transform(msg)
                                        .subscribe(new Consumer<XMessage>() {
                                            @Override
                                            public void accept(XMessage transformedMessage) {
                                                logTimeTaken(startTime, 2);
                                                if (transformedMessage != null) {
                                                    try {
                                                        kafkaProducer.send(outboundTopic, transformedMessage.toXML());
                                                        long endTime = System.nanoTime();
                                                        long duration = (endTime - startTime);
                                                        log.error("Total time spent in processing form: " + duration / 1000000);
                                                    } catch (JAXBException e) {
                                                        e.printStackTrace();
                                                    }

                                                }
                                            }
                                        });
                            }
                        } catch (JAXBException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) {
                        System.out.println(e.getMessage());
                        log.error("KafkaFlux exception", e);
                    }
                }).subscribe();

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

    @Override
    public Mono<XMessage> transform(XMessage xMessage) throws Exception {
        XMessage[] finalXMsg = new XMessage[1];
        return campaignService
                .getCampaignFromNameTransformer(xMessage.getApp())
                .map(new Function<JsonNode, Mono<Mono<Mono<XMessage>>>>() {
                    @Override
                    public Mono<Mono<Mono<XMessage>>> apply(JsonNode campaign) {
                        if (campaign != null) {
                            String formID = ODKConsumerReactive.this.getFormID(campaign);
                            if (formID.equals("")) {
                                log.error("Unable to find form ID from Conversation Logic");
                                return null;
                            }
                            String formPath = getFormPath(formID);
                            boolean isStartingMessage = xMessage.getPayload().getText().equals(campaign.findValue("startingMessage").asText());
                            switchFromTo(xMessage);

                            // Get details of user from database
                            return getPreviousMetadata(xMessage, formID)
                                    .map(new Function<FormManagerParams, Mono<Mono<XMessage>>>() {
                                        @Override
                                        public Mono<Mono<XMessage>> apply(FormManagerParams previousMeta) {
                                            final ServiceResponse[] response = new ServiceResponse[1];
                                            MenuManager mm;
                                            if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*") || isStartingMessage) {
                                                previousMeta.currentAnswer = "*";
                                                ServiceResponse serviceResponse = new MenuManager(null, null, null, formPath, formID, false, questionRepo).start();
                                                FormUpdation ss = FormUpdation.builder().build();
                                                ss.parse(serviceResponse.currentResponseState);
                                                ss.updateAdapterProperties(xMessage.getChannel(), xMessage.getProvider());
                                                String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                        ss.getXML();
                                                log.debug("Instance value >> " + instanceXMlPrevious);
                                                mm = new MenuManager(null, null, instanceXMlPrevious, formPath, formID, true, questionRepo);
                                                response[0] = mm.start();
                                            } else {
                                                mm = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer,
                                                        previousMeta.instanceXMlPrevious, formPath, formID, false, questionRepo);
                                                response[0] = mm.start();
                                            }

                                            // Save answerData => PreviousQuestion + CurrentAnswer
                                            Mono<Pair<Boolean, List<Question>>> updateQuestionAndAssessment =
                                                    updateQuestionAndAssessment(
                                                            previousMeta,
                                                            getPreviousQuestions(
                                                                    previousMeta.previousPath,
                                                                    formID,
                                                                    response[0].formVersion),
                                                            formID,
                                                            campaign,
                                                            xMessage,
                                                            response[0].question
                                                    );


                                            if (mm.isGlobal() && response[0].currentIndex.contains("eof__")) {
                                                String nextBotID = mm.getNextBotID(response[0].currentIndex);

                                                return Mono.zip(
                                                        campaignService.getBotNameByBotID(nextBotID),
                                                        campaignService.getFirstFormByBotID(nextBotID)
                                                ).map(new Function<Tuple2<String, String>, Mono<XMessage>>() {
                                                    @Override
                                                    public Mono<XMessage> apply(Tuple2<String, String> objects) {
                                                        String nextFormID = objects.getT2();
                                                        String nextAppName = objects.getT1();

                                                        ServiceResponse serviceResponse = new MenuManager(
                                                                null, null, null,
                                                                getFormPath(nextFormID), nextFormID,
                                                                false, questionRepo)
                                                                .start();
                                                        FormUpdation ss = FormUpdation.builder().build();
                                                        ss.parse(serviceResponse.currentResponseState);
                                                        ss.updateAdapterProperties(xMessage.getChannel(), xMessage.getProvider());
                                                        String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                                                ss.getXML();
                                                        log.debug("Instance value >> " + instanceXMlPrevious);
                                                        MenuManager mm2 = new MenuManager(null, null,
                                                                instanceXMlPrevious, getFormPath(nextFormID), nextFormID, true,
                                                                questionRepo);
                                                        ServiceResponse response = mm2.start();
                                                        xMessage.setApp(nextAppName);
                                                        return decodeXMessage(xMessage, response, nextFormID, updateQuestionAndAssessment);
                                                    }
                                                });
                                            } else {
                                                return Mono.just(decodeXMessage(xMessage, response[0], formID, updateQuestionAndAssessment));
                                            }
                                        }
                                    });
                        } else {
                            log.error("Could not find Bot");
                            return Mono.just(null);
                        }
                    }
                })
                .flatMap(new Function<Mono<Mono<Mono<XMessage>>>, Mono<XMessage>>() {
                    @Override
                    public Mono<XMessage> apply(Mono<Mono<Mono<XMessage>>> m) {
                        log.info("Level 1");
                        return m.flatMap(new Function<Mono<Mono<XMessage>>, Mono<? extends XMessage>>() {
                            @Override
                            public Mono<? extends XMessage> apply(Mono<Mono<XMessage>> n) {
                                log.info("Level 2");
                                return n.flatMap(new Function<Mono<XMessage>, Mono<? extends XMessage>>() {
                                    @Override
                                    public Mono<? extends XMessage> apply(Mono<XMessage> o) {
                                        return o;
                                    }
                                });
                            }
                        });
                    }
                });
    }

    private Mono<FormManagerParams> getPreviousMetadata(XMessage message, String formID) {
        String prevPath = null;
        String prevXMl = null;
        FormManagerParams formManagerParams = new FormManagerParams();

        if (!message.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            return stateRepo.findByPhoneNoAndBotFormName(message.getTo().getUserID(), formID)
                    .map(new Function<GupshupStateEntity, FormManagerParams>() {
                        @Override
                        public FormManagerParams apply(GupshupStateEntity stateEntity) {
                            String prevXMl = null, prevPath = null;
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
                            formManagerParams.setPreviousPath(prevPath);
                            formManagerParams.setInstanceXMlPrevious(prevXMl);
                            return formManagerParams;
                        }
                    })
                    .doOnError(e -> log.error(e.getMessage()));
        } else {
            formManagerParams.setCurrentAnswer("");
            formManagerParams.setPreviousPath(prevPath);
            formManagerParams.setInstanceXMlPrevious(prevXMl);
            return Mono.just(formManagerParams);
        }
    }

    @NotNull
    private Mono<Pair<Boolean, List<Question>>> updateQuestionAndAssessment(FormManagerParams previousMeta,
                                                                            Mono<Pair<Boolean, List<Question>>> previousQuestions, String formID,
                                                                            JsonNode campaign, XMessage xMessage, Question question) {
        return previousQuestions
                .doOnNext(new Consumer<Pair<Boolean, List<Question>>>() {
                    @Override
                    public void accept(Pair<Boolean, List<Question>> existingQuestionStatus) {
                        if (existingQuestionStatus.getLeft()) {
                            saveAssessmentData(
                                    existingQuestionStatus, formID, previousMeta, campaign, xMessage, null).subscribe(new Consumer<Assessment>() {
                                @Override
                                public void accept(Assessment assessment) {
                                    log.info("Assessment Saved Successfully {}", assessment.getId());
                                }
                            });
                        } else {
                            saveQuestion(question).subscribe(new Consumer<Question>() {
                                @Override
                                public void accept(Question question) {
                                    log.info("Question Saved Successfully");
                                    saveAssessmentData(
                                            existingQuestionStatus, formID, previousMeta, campaign, xMessage, question).subscribe(new Consumer<Assessment>() {
                                        @Override
                                        public void accept(Assessment assessment) {
                                            log.info("Assessment Saved Successfully {}", assessment.getId());
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
    }

    private Mono<Pair<Boolean, List<Question>>> getPreviousQuestions(String previousPath, String formID, String formVersion) {
        return questionRepo
                .findQuestionByXPathAndFormIDAndFormVersion(previousPath, formID, formVersion)
                .collectList()
                .flatMap(new Function<List<Question>, Mono<Pair<Boolean, List<Question>>>>() {
                    @Override
                    public Mono<Pair<Boolean, List<Question>>> apply(List<Question> questions) {
                        Pair<Boolean, List<Question>> response = Pair.of(false, new ArrayList<Question>());
                        if (questions != null && questions.size() > 0) {
                            response = Pair.of(true, questions);
                        }
                        return Mono.just(response);
                    }
                });
    }

    private Mono<Question> saveQuestion(Question question) {
        return questionRepo.save(question);
    }

    private Mono<Assessment> saveAssessmentData(Pair<Boolean, List<Question>> existingQuestionStatus,
                                                String formID, FormManagerParams previousMeta,
                                                JsonNode campaign, XMessage xMessage, Question question) {
        if (question == null) question = existingQuestionStatus.getRight().get(0);
        Assessment assessment = Assessment.builder()
                .question(question)
                .answer(previousMeta.currentAnswer)
                .botID(UUID.fromString(campaign.findValue("id").asText()))
                .build();
        try {
            String telemetryEvent = new AssessmentTelemetryBuilder()
                    .build("",
                            xMessage.getChannel(),
                            xMessage.getProvider(),
                            producerID,
                            "",
                            assessment.getQuestion(),
                            assessment,
                            0);
            kafkaProducer.send(telemetryTopic, telemetryEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return assessmentRepo.save(assessment)
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        log.error(throwable.getMessage());
                    }
                })
                .doOnNext(new Consumer<Assessment>() {
                    @Override
                    public void accept(Assessment assessment) {
                        log.info("Assessment Saved");
                    }
                });
    }

    private Mono<XMessage> decodeXMessage(XMessage xMessage, ServiceResponse response, String formID, Mono<Pair<Boolean, List<Question>>> updateQuestionAndAssessment) {
        XMessage nextMessage = getMessageFromResponse(xMessage, response);
        if (isEndOfForm(response)) {
            return Mono.zip(
                    appendNewResponse(formID, xMessage, response),
                    replaceUserState(formID, xMessage, response),
                    updateQuestionAndAssessment,
                    Mono.just(new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate))
            )
                    .then(Mono.just(getClone(nextMessage)));
        } else {
            return Mono.zip(
                    appendNewResponse(formID, xMessage, response),
                    replaceUserState(formID, xMessage, response),
                    updateQuestionAndAssessment
            )
                    .then(Mono.just(getClone(nextMessage)));
        }
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

    private XMessage getMessageFromResponse(XMessage xMessage, ServiceResponse response) {
        XMessagePayload payload = response.getNextMessage();
        xMessage.setPayload(payload);
        return xMessage;
    }

    public static String getFormPath(String formID) {
        FormsDao dao = new FormsDao(JsonDB.getInstance().getDB());
        return dao.getFormsCursorForFormId(formID).getFormFilePath();
    }

    private Mono<GupshupMessageEntity> appendNewResponse(String formID, XMessage xMessage, ServiceResponse response) {
        GupshupMessageEntity msgEntity = new GupshupMessageEntity();
        msgEntity.setPhoneNo(xMessage.getTo().getUserID());
        msgEntity.setMessage(xMessage.getPayload().getText());
        msgEntity.setLastResponse(response.getCurrentIndex().equals("endOfForm"));
        return msgRepo.save(msgEntity);
    }

    private Mono<GupshupStateEntity> replaceUserState(String formID, XMessage xMessage, ServiceResponse response) {
        return stateRepo.findByPhoneNoAndBotFormName(xMessage.getTo().getUserID(), formID)
                .map(new Function<GupshupStateEntity, Mono<GupshupStateEntity>>() {
                    @Override
                    public Mono<GupshupStateEntity> apply(GupshupStateEntity saveEntity) {
                        if (saveEntity == null) {
                            saveEntity = new GupshupStateEntity();
                        }
                        saveEntity.setPhoneNo(xMessage.getTo().getUserID());
                        saveEntity.setPreviousPath(response.getCurrentIndex());
                        saveEntity.setXmlPrevious(response.getCurrentResponseState());
                        saveEntity.setBotFormName(formID);
                        return stateRepo.save(saveEntity);
                    }
                }).flatMap(new Function<Mono<GupshupStateEntity>, Mono<? extends GupshupStateEntity>>() {
                    @Override
                    public Mono<? extends GupshupStateEntity> apply(Mono<GupshupStateEntity> gupshupStateEntityMono) {
                        return gupshupStateEntityMono;
                    }
                });

    }

    private void logTimeTaken(long startTime, int checkpointID) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        log.info(String.format("CP-%d: %d ms", checkpointID, duration));
    }
}