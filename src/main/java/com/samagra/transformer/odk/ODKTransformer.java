package com.samagra.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.odk.entity.GupshupMessageEntity;
import com.samagra.transformer.odk.entity.GupshupStateEntity;
import com.samagra.transformer.odk.persistance.FormsDao;
import com.samagra.transformer.odk.persistance.JsonDB;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.pt.skills.CandidateApproval;
import com.samagra.transformer.pt.skills.EmployerRegistration;
import com.samagra.transformer.samagra.LeaveManager;
import com.samagra.transformer.samagra.SamagraOrgForm;
import com.samagra.transformer.samagra.TemplateServiceUtils;
import com.samagra.transformer.publisher.CommonProducer;
import com.uci.dao.models.XMessageDAO;
import com.uci.utils.CampaignService;
import io.fusionauth.domain.User;
import lombok.*;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static messagerosa.core.model.XMessage.MessageState.NOT_SENT;
import static messagerosa.core.model.XMessage.MessageType.HSM;

@Slf4j
@Component
public class ODKTransformer extends TransformerProvider {

    private static final String SMS_BROADCAST_IDENTIFIER = "Broadcast";

    @Autowired
    public CommonProducer kafkaProducer;

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

    // Listen to topic "Forms"

    // Gets the message => Calls transform() =>  Calls xMessage.completeTransform() =>  send it to inbound-unprocessed


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
//        JsonNode campaign =
        return campaignService.getCampaignFromNameTransformer(xMessage.getApp()).flatMap(new Function<JsonNode, Mono<? extends XMessage>>() {
            @Override
            public Mono<XMessage> apply(JsonNode campaign) {
                if (campaign != null) {
                    String formID = getFormID(campaign);
                    String formPath = getFormPath(formID);
                    boolean isStartingMessage = xMessage.getPayload().getText().equals(campaign.findValue("startingMessage").asText());
                    boolean isPrefilled = false;

                    // Switch from-to
                    switchFromTo(xMessage);

                    // Get details of user from database
                    FormManagerParams previousMeta = getPreviousMetadata(xMessage, formID);

                    boolean isApprovalFlow = false;
                    User employee = null;


                    // TODO Make a distinction between Form and MenuManager based on Campaign Configuration.
                    if (isSamagraBot(formID)) {
                        isPrefilled = true;
                        employee = UserService.findByPhoneAndCampaign(xMessage.getTo().getUserID(), campaign);
                        if (xMessage.getPayload() != null) {
                            isApprovalFlow = approvalFlow(employee, xMessage.getPayload().getText(), xMessage);
                        }

                        if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*") || isStartingMessage) {
                            LeaveManager.builder().user(employee).build().updateLeaves(0);
                            SamagraOrgForm orgForm = SamagraOrgForm.builder().user(employee).build();
                            String instanceXML = orgForm.getInitialValue();
                            log.info("Current InstanceXML :: " + instanceXML);
                            previousMeta.instanceXMlPrevious = instanceXML;
                        }

                        if (previousMeta.currentAnswer.equals("#")) {
                            //Update leaves
                            SamagraOrgForm orgForm = SamagraOrgForm.builder().build();
                            orgForm.setUser(employee);
                            orgForm.parse(previousMeta.instanceXMlPrevious);
                            previousMeta.instanceXMlPrevious = orgForm.updateLeaves().replaceAll("__", "_");
                        }
                    }


                    if (isSakshamSamikshaBot(formID)) {
                        isPrefilled = true;
                        if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*") || isStartingMessage) {
                            previousMeta.currentAnswer = "*";
                            ServiceResponse response = new MenuManager(null, null, null, formPath, isPrefilled).start();
//                    SakshamSamiksha ss = SakshamSamiksha.builder().applicationID(campaign.id.toString()).phone(xMessage.getTo().getUserID()).build();
//                    ss.parse(response.currentResponseState);
//                    previousMeta.instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ss.getInitialValue().replaceAll("__", "_");
                        }
//                if (previousMeta.currentAnswer.equals("#")) {
//                    SakshamSamiksha ss = SakshamSamiksha.builder().applicationID(campaign.id.toString()).phone(xMessage.getTo().getUserID()).build();
//                    ss.parse(previousMeta.instanceXMlPrevious);
//                    previousMeta.instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ss.getInitialValue().replaceAll("__", "_");
//                }
                    }

                    if (!isApprovalFlow) {

                        final ServiceResponse[] response = new ServiceResponse[1];
                        MenuManager mm;
                        if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*") || isStartingMessage) {
                            previousMeta.currentAnswer = "*";
                            mm = new MenuManager(null, null, null, formPath, false);
                            if (formID.equals("Rozgar-Saathi-MVP-EmpReg-Vac-Chatbot4")) {
                                response[0] = mm.start();
                                EmployerRegistration ss = EmployerRegistration.builder().phone(xMessage.getTo().getUserID()).build();
                                ss.parse(response[0].currentResponseState);
                                String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ss.updatePhoneNumber(xMessage.getTo().getUserID()).getXML();
                                mm = new MenuManager(null, null, instanceXMlPrevious, formPath, true);
                            }
                            response[0] = mm.start();

                        } else {
                            mm = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer, previousMeta.instanceXMlPrevious, formPath, false);
                            response[0] = mm.start();
                        }

                        if (mm.isGlobal() && response[0].currentIndex.contains("eof__")) {
                            String nextBotID = mm.getNextBotID(response[0].currentIndex);
//                    String nextFormID =
                            User finalEmployee = employee;
                            return campaignService.getFirstFormByBotID(nextBotID).map(new Function<String, XMessage>() {
                                                                                   @Override
                                                                                   public XMessage apply(String nextFormID) {
                                                                                       MenuManager mm2 = new MenuManager(null, null, null, getFormPath(nextFormID), false);
                                                                                       response[0] = mm2.start();
                                                                                       try {
                                                                                           return decodeXMessage(xMessage, response[0], formID, previousMeta, finalEmployee);
                                                                                       } catch (Exception e) {
                                                                                           return null;
                                                                                       }
                                                                                   }
                                                                               }

                            );

                        } else {
                            try {
                                return Mono.just(decodeXMessage(xMessage, response[0], formID, previousMeta, employee));
                            } catch (Exception e) {

                                e.printStackTrace();
                                return null;
                            }
                        }

                        // Create new xMessage from response

                    }
                }
                return null;
            }
        });

    }

    private XMessage decodeXMessage(XMessage xMessage, ServiceResponse response, String formID, FormManagerParams previousMeta, User employee) throws Exception {
        XMessage nextMessage = getMessageFromResponse(xMessage, response);

        // Update database with new fields.
        appendNewResponse(formID, xMessage, response);
        replaceUserState(formID, xMessage, response);

        XMessage cloneMessage = getClone(nextMessage);

        if (!isSamagraBot(formID) && isEndOfForm(response)) {
            new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate);
        }

        if (isSamagraBot(formID) && !previousMeta.currentAnswer.equals("#")) {
            SamagraOrgForm orgForm = null;
            orgForm = SamagraOrgForm.builder().build();
            orgForm.parse(response.currentResponseState);
            if (isEndOfForm(response)) {
                new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate);
                if (response.currentIndex.contains("eof_leave_applied_message")) {
                    // Send message to manager

                    // Check for program construct.
                    String construct = UserService.getProgramConstruct(employee);
                    Boolean isAssociate = UserService.isAssociate(employee);
                    if (construct.equals("2") && isAssociate) {
                        User coordinator = UserService.getProgramCoordinator(employee);
                        sendMessageToManagerForApproval(employee, cloneMessage, orgForm, coordinator);
                    } else {
                        User manager = UserService.getManager(employee);
                        sendMessageToManagerForApproval(employee, cloneMessage, orgForm, manager);
                        oneTimeSampleTask(employee.fullName, (String) employee.data.get("engagement"), orgForm.getStartDate(), "3");
                    }
                } else if (response.currentIndex.contains("eof_air_ticket_applied_message_one_way")) {
                    sendMessageForFlightOneWayToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_air_ticket_applied_message_two_way")) {
                    sendMessageForFlightTwoWayToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_air_ticket_amendment_note")) {
                } else if (response.currentIndex.contains("eof_air_ticket_cancellation_note")) {
                    sendCancellationMessageToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_missed_flight_note")) {
                    sendMissedFlightMessageToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_train_ticket_applied_message_one_way")) {
                    sendOneWayMessageForTrainToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_train_ticket_applied_message_two_way")) {
                    sendTwoWayMessageForTrainToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_train_ticket_cancellation_note")) {
                    sendTrainCancellationMessageToAdmin(orgForm, cloneMessage, employee);
                } else if (response.currentIndex.contains("eof_train_missed_note")) {
                    sendTrainMissedMessageToAdmin(orgForm, cloneMessage, employee);
                } else {
                    log.info("Unable to find any any endOfForm cased that were mentioned.");
                }
            }
        }
        return nextMessage;
    }

    private boolean isEndOfForm(ServiceResponse response) {
        return response.getCurrentIndex().equals("endOfForm") || response.currentIndex.contains("eof");
    }

    private boolean isSamagraBot(String formID) {
        return formID.equals("samagra_workflows_form_updated_6");
    }

    private boolean isSakshamSamikshaBot(String formID) {
        return formID.equals("schoolheads_v1");
    }

    private boolean isMissionPrerna(String formID) {
        return formID.equals("ss_form_mpc");
    }

    private void sendTrainMissedMessageToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Raju Ram", "SamagraBot");
            if (admin != null) {
                String missedFlightMessage = TemplateServiceUtils.getFormattedString("TrainMissedMessage", employee.fullName, orgForm.getTrainMissedPNR());
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(missedFlightMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTrainCancellationMessageToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Raju Ram", "SamagraBot");
            if (admin != null) {
                String missedFlightMessage = TemplateServiceUtils.getFormattedString(
                        "TicketCancellationMesssage", employee.fullName, orgForm.getTrainCancellationPNR());
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(missedFlightMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTwoWayMessageForTrainToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Raju Ram", "SamagraBot");
            if (admin != null) {
                String onwardDate = (String) orgForm.getTrainOneWayData().get("start_date_train");
                String returnDate = (String) orgForm.getTrainTwoWayData().get("start_date_return");
                String startCity = (String) orgForm.getTrainOneWayData().get("start_city_name_one_way_train");
                String destinationCity = (String) orgForm.getTrainOneWayData().get("end_city_name_one_way_train");
                String onwardTrainNumber = (String) orgForm.getTrainOneWayData().get("train_name_one_way_name");
                String returnTrainNumber = (String) orgForm.getTrainOneWayData().get("train_name_return_way_name");
                String oneWayTripMessage = TemplateServiceUtils.getFormattedString(
                        "TwoWayTrainTicketMessage", employee.fullName,
                        onwardDate, returnDate, startCity, destinationCity, onwardTrainNumber, returnTrainNumber);
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(oneWayTripMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendOneWayMessageForTrainToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Raju Ram", "SamagraBot");
            if (admin != null) {
                String travelDate = (String) orgForm.getTrainOneWayData().get("start_date_train");
                String startCity = (String) orgForm.getTrainOneWayData().get("start_city_name_one_way_train");
                String destinationCity = (String) orgForm.getTrainOneWayData().get("end_city_name_one_way_train");
                String trainNumber = (String) orgForm.getTrainOneWayData().get("train_name_one_way_name");
                String oneWayTripMessage = TemplateServiceUtils.getFormattedString(
                        "OneWayTrainTicketMessage", employee.fullName,
                        travelDate, startCity, destinationCity, trainNumber);
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(oneWayTripMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFormID(JsonNode campaign) {
        try {
            return campaign.findValue("formID").asText();
        } catch (Exception e) {
            return "";
        }
    }

    private void sendMessageToManagerForApproval(User employee, XMessage nextMessage, SamagraOrgForm orgForm, User manager) throws Exception {
        String getLeaveMessage = TemplateServiceUtils.getFormattedString(
                "LeaveMessage", manager.fullName, employee.fullName, orgForm.getReason(),
                orgForm.getStartDate(), orgForm.getEndDate(), orgForm.getNumberOfWorkingDays(),
                orgForm.getReasonForLeave());
        XMessagePayload payload = XMessagePayload.builder().text(getLeaveMessage).build();
        nextMessage.setPayload(payload);
        nextMessage.setMessageType(HSM);
        nextMessage.getTo().setUserID(manager.mobilePhone);
        sendSingle(nextMessage);
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

    private void sendMissedFlightMessageToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Sanchita Dasgupta", "SamagraBot");
            if (admin != null) {
                String missedFlightMessage = TemplateServiceUtils.getFormattedString(
                        "MissedFlightMessage", employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(missedFlightMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCancellationMessageToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Sanchita Dasgupta", "SamagraBot");
            if (admin != null) {
                String missedFlightMessage = TemplateServiceUtils.getFormattedString(
                        "TicketCancellationMesssage", employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(missedFlightMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageForFlightOneWayToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Sanchita Dasgupta", "SamagraBot");
            if (admin != null) {
                String travelDate = (String) orgForm.getAirOneWayData().get("start_date");
                String startCity = (String) orgForm.getAirOneWayData().get("start_city_name_one_way");
                String destinationCity = (String) orgForm.getAirOneWayData().get("end_city_name_one_way");
                String flightNumber = (String) orgForm.getAirOneWayData().get("enter_onward_flight");
                String oneWayTripMessage = TemplateServiceUtils.getFormattedString("OneWayTripMessage", employee.fullName,
                        travelDate, startCity, destinationCity, flightNumber);
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(oneWayTripMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageForFlightTwoWayToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Sanchita Dasgupta", "SamagraBot");
            if (admin != null) {
                String travelDate = (String) orgForm.getAirOneWayData().get("start_date");
                String returnDate = (String) orgForm.getAirTwoWayData().get("end_date");
                String startCity = (String) orgForm.getAirOneWayData().get("start_city_name_one_way");
                String destinationCity = (String) orgForm.getAirOneWayData().get("end_city_name_one_way");
                String flightNumber = (String) orgForm.getAirOneWayData().get("enter_onward_flight");
                String returnFlightNumber = (String) orgForm.getAirTwoWayData().get("enter_return_flight");
                String twoWayTripMessage = TemplateServiceUtils.getFormattedString("TwoWayTripMessage", employee.fullName,
                        travelDate, returnDate, startCity, destinationCity, flightNumber, returnFlightNumber);
                switchFromTo(message);
                message.setMessageType(HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(twoWayTripMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchFromTo(XMessage xMessage) {
        SenderReceiverInfo from = xMessage.getFrom();
        SenderReceiverInfo to = xMessage.getTo();
        xMessage.setFrom(to);
        xMessage.setTo(from);
    }

    private void sendSingle(XMessage nextMessage) {
        try {
            log.error("SendSingle");
            log.error(nextMessage.toXML());
            log.error("________________________________________");
            kafkaProducer.send("outbound", nextMessage.toXML());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private boolean approvalFlow(User manager, String payload, XMessage xmsg) {

        boolean isApprovalFlow = false;
        // Check if the response is from a manager.
        // Get last thing sent to the manager
        try {
            String message = getLastSentMessage(xmsg.getMessageId().getReplyId(), manager.mobilePhone).getXMessage();
            if (message.contains("1. Approve")) {
                respondToManager(manager, getClone(xmsg));
                isApprovalFlow = true;
                // Get the employee for that manager.
                String employeeName = getEmployeeNameFromMessage(message);
                String startDateString = getStartingDateFromMessage(message);
                String endDateString = getEndDateFromMessage(message);
                String workingDays = getWorkingDaysFromMessage(message);
                try {
                    User employee = UserService.getUserByFullName(employeeName, "SamagraBot");

                    boolean isApproved = payload.equals("1");

                    if (isApproved)
                        approvalPipeline(manager, xmsg, startDateString, endDateString, workingDays, employee);
                    else rejectionPipeline(xmsg, startDateString, employee);

                    // Update the previously submitted form for employee.
                    updateLastSentForm(employee);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            return false;
        }
        return isApprovalFlow;
    }

    private String getWorkingDaysFromMessage(String message) {
        return message.split(" working days ")[0].split(" for ")[2];
    }

    private String getEndDateFromMessage(String message) {
        return message.split("leave from ")[1].split(" to ")[1].split(" for ")[0];
    }

    private String getStartingDateFromMessage(String message) {
        return message.split("leave from ")[1].split(" to ")[0];
    }

    private String getEmployeeNameFromMessage(String message) {
        return message.split(" from your team has applied for")[0].split("this is to inform you that ")[1];
    }

    private void rejectionPipeline(XMessage xmsg, String startDateString, User employee) throws Exception {
        // If rejected => update the end user;
        buildRejectionMessage(employee, getClone(xmsg));
        oneTimeSampleTask(employee.fullName, (String) employee.data.get("engagement"), startDateString, "2");
    }

    private void approvalPipeline(User manager, XMessage xmsg, String startDateString, String endDateString, String workingDays, User employee) throws Exception {
        // If approved => update leave status; update the end user;
        LeaveManager.builder().user(employee).build().updateLeaves(Integer.parseInt(workingDays));
        buildApprovalMessage(employee, getClone(xmsg));
        buildProgramOwnerMessage(employee, getClone(xmsg), startDateString, endDateString, workingDays);
        deleteLastMessage(manager);
        oneTimeSampleTask(employee.fullName, (String) employee.data.get("engagement"), startDateString, "1");


    }

    private void oneTimeSampleTask(String employeeName, String teamName, String startDateString, String status) {
        log.info("++++++++++++++++++++++++++++++++++++");
        FailureHandler failureHandler = new FailureHandler.OnFailureRetryLater(Duration.ofSeconds(20));
        SchedulerData schedulerData = new SchedulerData(employeeName, teamName, startDateString, status);

        OneTimeTask<SchedulerData> oneTimeTask = new OneTimeTask<SchedulerData>("one-time-" + UUID.randomUUID().toString(), SchedulerData.class, failureHandler, new DeadExecutionHandler<SchedulerData>() {
            @Override
            public void deadExecution(Execution execution, ExecutionOperations<SchedulerData> executionOperations) {

            }
        }) {
            @SneakyThrows
            @Override
            public void executeOnce(TaskInstance<SchedulerData> taskInstance, ExecutionContext executionContext) {
                log.info("Running recurring-simple-task. Instance: {}, ctx: {}", taskInstance, executionContext);
                try {
                    updateStatusForApproval(
                            taskInstance.getData().getEmployeeName(),
                            taskInstance.getData().getTeamName(),
                            taskInstance.getData().getStartDateString(),
                            taskInstance.getData().getStatus()
                    );
                } catch (Exception e) {

                    log.error("Exception in task");
                    log.error(e.getMessage());
                    throw new Exception("Failed to execute => So retrying"); //For retrying
                }
            }
        };
        final Scheduler scheduler = Scheduler
                .create(dataSource, oneTimeTask)
                .serializer(new DbSchedulerJsonSerializer())
                .threads(2)
                .build();

        scheduler.start();

        // Schedule the task for execution a certain time in the future and optionally provide custom data for the execution
        scheduler.schedule(oneTimeTask.instance(UUID.randomUUID().toString(), schedulerData), Instant.now().plusSeconds(5));
    }

    public static int retry = 0;

    private void updateStatusForApproval(String employeeName, String teamName, String startDateString, String approvalStatus) throws IOException {
        log.info("starting task ===============================================" + employeeName + "-" + teamName + "-" + startDateString + "-" + approvalStatus);
        String baseURL = "http://139.59.93.172:3000/samagra-internal-workflow-v2?filter=";
        String filters = String.format("{\"where\":{\"data.member_name\": \"%s\", \"data.team_name\": \"%s\", \"data.start_date_leave\": \"%s\"}}",
                employeeName, teamName, startDateString);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(baseURL + filters)
                .method("GET", null)
                .build();
        log.info("Currently calling::" + baseURL + filters);
        Response response = client.newCall(request).execute();
        String jsonData = response.body().string();
        JSONArray jsonArray = new JSONArray(jsonData);
        JSONObject data = (JSONObject) jsonArray.get(0);


        String oldData = null;
        try {
            oldData = ((JSONObject) data.getJSONArray("data").get(0)).getString("manager_approval");
        } catch (Exception e) {
        }
        log.info("OldAns :: " + oldData);
        if (approvalStatus.equals("3") && oldData == null) {
            ((JSONObject) data.getJSONArray("data").get(0)).put("manager_approval", approvalStatus);
        } else if ((oldData == null || oldData.equals("2") || oldData.equals("1")) && approvalStatus.equals("2")) {
            ((JSONObject) data.getJSONArray("data").get(0)).put("manager_approval", approvalStatus);
        } else if ((oldData == null || oldData.equals("3") || oldData.equals("2")) && approvalStatus.equals("1")) {
            ((JSONObject) data.getJSONArray("data").get(0)).put("manager_approval", approvalStatus);
        } else {
            return;
        }


        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, data.toString());

        Request request2 = new Request.Builder()
                .url("http://139.59.93.172:3000/samagra-internal-workflow-v2/" + data.get("id"))
                .method("PUT", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response2 = client.newCall(request2).execute();
        String updateResponse = response2.body().string();
        log.info(updateResponse);
        log.info("task completed ===============================================");
    }

    private void respondToManager(User manager, XMessage message) throws Exception {
        String approvalMessage = TemplateServiceUtils.getFormattedString("ManagerAcknowledgementMessage", manager.fullName);
        message.setMessageType(HSM);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    // Move all these to a separate transformer

    private void updateLastSentForm(User user) {
    }

    private void buildRejectionMessage(User user, XMessage message) throws Exception {
        String approvalMessage = TemplateServiceUtils.getFormattedString("RejectionStatusMessage", user.fullName, "Rejected", (String) user.data.get("reportingManager"));
        switchFromTo(message);
        message.setMessageType(HSM);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private void buildProgramOwnerMessage(User user, XMessage message, String startDate, String endDate, String numberOfdays) throws Exception {
        User owner;
        if (UserService.getProgramConstruct(user).equals("2") && UserService.isAssociate(user)) {
            owner = UserService.getManager(user);
        } else owner = UserService.getEngagementOwner(user);
        if (owner != null) {
            String approvalMessage = TemplateServiceUtils.getFormattedString("POReportMessage", owner.fullName, user.fullName,
                    (String) user.data.get("engagement"), startDate, endDate, numberOfdays);
            switchFromTo(message);
            message.setMessageType(HSM);
            message.getTo().setUserID(owner.mobilePhone);
            message.getPayload().setText(approvalMessage);
            sendSingle(message);
        }
    }

    private void buildApprovalMessage(User user, XMessage message) throws Exception {
        User owner;
        if (UserService.getProgramConstruct(user).equals("2") && UserService.isAssociate(user)) {
            owner = UserService.getManager(user);
        } else owner = UserService.getEngagementOwner(user);

        String approvalMessage = TemplateServiceUtils.getFormattedString("ApprovalStatus", user.fullName, "Approved", owner.fullName);
        switchFromTo(message);
        message.setMessageType(HSM);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private XMessageDAO getLastSentMessage(String replyId, String userId) {
        String url = String.format("http://orchestrator:8686/getLastMessage?replyId=%s&userId=%s", replyId, userId);
        return restTemplate.getForEntity(url, XMessageDAO.class).getBody();
    }

    private void deleteLastMessage(User user) {
        String url = String.format("http://orchestrator:8686/deleteLastMessage?userID=%s&messageType=REPLIED", user.mobilePhone);
        restTemplate.getForEntity(url, Object.class);
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
                    ServiceResponse response = new MenuManager(null, null, null, formPath, false).start();
                    CandidateApproval ss = CandidateApproval.builder().applicationID(campaignID).phone(userPhone).build();
                    ss.parse(response.currentResponseState);
                    String instanceXMlPrevious = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ss.updateHiddenFields(hiddenFields, (JSONObject) users.get(i)).getXML();
                    MenuManager mm = new MenuManager(null, null, instanceXMlPrevious, formPath, true);
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