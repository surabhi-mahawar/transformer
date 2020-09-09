package com.samagra.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.User.CampaignService;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.odk.entity.GupshupMessageEntity;
import com.samagra.transformer.odk.entity.GupshupStateEntity;
import com.samagra.transformer.odk.persistance.FormsDao;
import com.samagra.transformer.odk.persistance.JsonDB;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.samagra.LeaveManager;
import com.samagra.transformer.samagra.SamagraOrgForm;
import com.samagra.transformer.samagra.TemplateServiceUtills;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.xml.XMessageParser;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.samagra.transformer.publisher.CommonProducer;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.util.*;


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

    @KafkaListener(id = "transformer1", topics = "Form2")
    public void consumeMessage(String message) throws Exception {
        long startTime = System.nanoTime();
        log.info("Form Transformer Message: " + message);
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        XMessage transformedMessage = this.transform(xMessage);
        if (transformedMessage != null) {
            log.error("mainSender");
            log.error(transformedMessage.toXML());
            log.error("________________________________________");
            kafkaProducer.send("outbound", transformedMessage.toXML());
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);
            log.error("Total time spent in processing form: " + duration / 1000000);
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
    public XMessage transform(XMessage xMessage) throws Exception {
        Application campaign = CampaignService.getCampaignFromName(xMessage.getApp());
        if (campaign != null) {
            String formID = getFormID(campaign);
            String formPath = getFormPath(formID);
            boolean isStartingMessage = xMessage.getPayload().getText().equals(campaign.data.get("startingMessage"));
            if (isStartingMessage) {
                xMessage.setPayload(null);
            }
            // Switch from-to
            switchFromTo(xMessage);

            // Get details of user from database
            FormManagerParams previousMeta = getPreviousMetadata(xMessage, formID);

            boolean isApprovalFlow = false;
            User employee = null;
            // TODO Make a distinction between Form and MenuManager based on Campaign Configuration.
            if (formID.equals("samagra_workflows_form_updated_2")) {

                employee = UserService.findByPhone(xMessage.getTo().getUserID());
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

            if (!isApprovalFlow) {

                ServiceResponse response = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer, previousMeta.instanceXMlPrevious, formPath).start();

                // Create new xMessage from response
                XMessage nextMessage = getMessageFromResponse(xMessage, response);

                // Update database with new fields.
                appendNewResponse(formID, xMessage, response);
                replaceUserState(formID, xMessage, response);

                XMessage cloneMessage = getClone(nextMessage);

                if (formID.equals("samagra_workflows_form_updated_2") && !previousMeta.currentAnswer.equals("#")) {
                    SamagraOrgForm orgForm = null;
                    orgForm = SamagraOrgForm.builder().build();
                    orgForm.parse(response.currentResponseState);
                    if (response.getCurrentIndex().equals("endOfForm") || response.currentIndex.contains("eof")) {
                        new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate);
                        if (response.currentIndex.contains("eof_leave_applied_message")) {
                            // Send message to manager
                            User manager = UserService.getManager(employee);
                            sendMessageToManagerForApproval(employee, cloneMessage, orgForm, manager);
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
        }
        return null;
    }

    private void sendTrainMissedMessageToAdmin(SamagraOrgForm orgForm, XMessage message, User employee) {
        try {
            User admin = UserService.getUserByFullName("Raju Ram", "SamagraBot");
            if (admin != null) {
                String missedFlightMessage = TemplateServiceUtills.getFormattedString("TrainMissedMessage", employee.fullName, orgForm.getTrainMissedPNR());
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String missedFlightMessage = TemplateServiceUtills.getFormattedString(
                        "TrainMissedMessage", employee.fullName, orgForm.getTrainCancellationPNR());
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String oneWayTripMessage = TemplateServiceUtills.getFormattedString(
                        "TwoWayTrainTicketMessage", employee.fullName,
                        onwardDate, returnDate, startCity, destinationCity, onwardTrainNumber, returnTrainNumber);
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String oneWayTripMessage = TemplateServiceUtills.getFormattedString(
                        "OneWayTrainTicketMessage", employee.fullName,
                travelDate, startCity, destinationCity, trainNumber);
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
                message.getTo().setUserID(admin.mobilePhone);
                message.getPayload().setText(oneWayTripMessage);
                sendSingle(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFormID(Application campaign) {
        return (String) ((Map<Object, Object>) ((ArrayList<Map>) campaign.data.get("parts")).get(0).get("meta")).get("formID");
    }

    private void sendMessageToManagerForApproval(User employee, XMessage nextMessage, SamagraOrgForm orgForm, User manager) throws Exception {
        String getLeaveMessage = TemplateServiceUtills.getFormattedString(
                "LeaveMessage", manager.fullName, employee.fullName, orgForm.getReason(),
                orgForm.getStartDate(), orgForm.getEndDate(), orgForm.getNumberOfWorkingDays(),
                orgForm.getReasonForLeave());
        XMessagePayload payload = XMessagePayload.builder().text(getLeaveMessage).build();
        nextMessage.setPayload(payload);
        nextMessage.setMessageType(XMessage.MessageType.HSM);
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
                String missedFlightMessage = TemplateServiceUtills.getFormattedString(
                        "MissedFlightMessage", employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String missedFlightMessage = TemplateServiceUtills.getFormattedString(
                        "TicketCancellationMesssage", employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String oneWayTripMessage = TemplateServiceUtills.getFormattedString("OneWayTripMessage", employee.fullName,
                        travelDate, startCity, destinationCity, flightNumber);
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
                String twoWayTripMessage = TemplateServiceUtills.getFormattedString("TwoWayTripMessage", employee.fullName,
                        travelDate, returnDate, startCity, destinationCity, flightNumber, returnFlightNumber);
                switchFromTo(message);
                message.setMessageType(XMessage.MessageType.HSM);
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
            String message = getLastSentMessage(xmsg.getMessageId().getReplyId()).getXMessage();
            if (message.contains("1. Approve")) {
                respondToManager(manager, getClone(xmsg));
                isApprovalFlow = true;
                // Get the employee for that manager.
                String employeeName = message.split(" from your team has applied for")[0].split("this is to inform you that ")[1];
                String startDateString = message.split("leave from ")[1].split(" to ")[0];
                String endDateString = message.split("leave from ")[1].split(" to ")[1].split(" for ")[0];
                String workingDays = message.split(" working days ")[0].split(" for ")[2];

                try {
                    User employee = UserService.getUserByFullName(employeeName, "SamagraBot");
                    boolean isApproved = payload.equals("1");
                    if (isApproved) { // If approved => update leave status; update the end user;
                        LeaveManager.builder().user(employee).build().updateLeaves(Integer.parseInt(workingDays));
                        buildApprovalMessage(employee, getClone(xmsg));
                        buildProgramOwnerMessage(employee, getClone(xmsg), startDateString, endDateString, workingDays);
                        deleteLastMessage(manager);
                        updateStatusForApproval(employee.fullName, (String) employee.data.get("engagement"), startDateString);
                    } else {// If rejected => update the end user;
                        buildRejectionMessage(employee, getClone(xmsg));
                    }

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

    private void updateStatusForApproval(String employeeName, String teamName, String startDateString) {
        String baseURL = "http://139.59.93.172:3000/samagra-internal-workflow-v2?filter=";
        String filters = String.format("{\"where\":{\"data.member_name\": \"%s\", \"data.team_name\": \"%s\", \"data.start_date_leave\": \"%s\"}}",
                employeeName, teamName, startDateString);
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url(baseURL + filters)
                    .method("GET", null)
                    .build();
            Response response = client.newCall(request).execute();
            String jsonData = response.body().string();
            JSONArray jsonArray = new JSONArray(jsonData);
            JSONObject data = (JSONObject) jsonArray.get(0);

            ((JSONObject) data.getJSONArray("data").get(0)).put("manager_approval", true);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, data.toString());

            Request request2 = new Request.Builder()
                    .url("http://139.59.93.172:3000/samagra-internal-workflow-v2/" + data.get("id"))
                    .method("PUT", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response2 = client.newCall(request2).execute();
            String updateResponse = response2.body().string();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void respondToManager(User manager, XMessage message) throws Exception {
        String approvalMessage = TemplateServiceUtills.getFormattedString("ManagerAcknowledgementMessage", manager.fullName);
        message.setMessageType(XMessage.MessageType.HSM);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    // Move all these to a separate transformer

    private void updateLastSentForm(User user) {
    }

    private void buildRejectionMessage(User user, XMessage message) throws Exception {
        String approvalMessage = TemplateServiceUtills.getFormattedString("RejectionStatusMessage", user.fullName, "Rejected", (String) user.data.get("reportingManager"));
        switchFromTo(message);
        message.setMessageType(XMessage.MessageType.HSM);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private void buildProgramOwnerMessage(User user, XMessage message, String startDate, String endDate, String numberOfdays) throws Exception {
        User owner = UserService.getEngagementOwner(user);
        if (owner != null) {
            String approvalMessage = TemplateServiceUtills.getFormattedString("POReportMessage", owner.fullName, user.fullName,
                    (String) user.data.get("engagement"), startDate, endDate, numberOfdays);
            switchFromTo(message);
            message.setMessageType(XMessage.MessageType.HSM);
            message.getTo().setUserID(owner.mobilePhone);
            message.getPayload().setText(approvalMessage);
            sendSingle(message);
        }
    }

    private void buildApprovalMessage(User user, XMessage message) throws Exception {
        String approvalMessage = TemplateServiceUtills.getFormattedString("ApprovalStatus", user.fullName, "Approved", (String) user.data.get("programOwner"));
        switchFromTo(message);
        message.setMessageType(XMessage.MessageType.HSM);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private XMessageDAO getLastSentMessage(String replyId) {
        String url = String.format("http://localhost:8081/getLastMessage?replyId=%s", replyId);
        return restTemplate.getForEntity(url, XMessageDAO.class).getBody();
    }

    private void deleteLastMessage(User user) {
        String url = String.format("http://localhost:8081/deleteLastMessage?userID=%s&messageType=REPLIED", user.mobilePhone);
        restTemplate.getForEntity(url, Object.class);
    }

    @Override
    public List<XMessage> transformToMany(XMessage xMessage) {
        return null;
    }

    private XMessage getMessageFromResponse(XMessage xMessage, ServiceResponse response) {
        // Add payload to the response
        XMessagePayload payload = XMessagePayload.builder()
                .text(response.getNextMessage())
                .build();
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
