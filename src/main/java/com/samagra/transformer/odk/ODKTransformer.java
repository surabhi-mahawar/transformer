package com.samagra.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.odk.entity.GupshupMessageEntity;
import com.samagra.transformer.odk.entity.GupshupStateEntity;
import com.samagra.transformer.odk.persistance.FormsDao;
import com.samagra.transformer.odk.persistance.JsonDB;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.samagra.LeaveManager;
import com.samagra.transformer.samagra.SamagraOrgForm;
import com.samagra.transformer.samagra.TemplateService;
import io.fusionauth.domain.User;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.samagra.transformer.publisher.CommonProducer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.samagra.transformer.User.UserService.getInfoForUser;

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
        log.error("mainSender");
        log.error(transformedMessage.toXML());
        log.error("________________________________________");
        kafkaProducer.send("outbound", transformedMessage.toXML());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        log.error("Total time spent in processing form: " + duration / 1000000);
    }

    // Listen to topic "Forms"

    // Gets the message => Calls transform() =>  Calls xMessage.completeTransform() =>  send it to inbound-unprocessed


    private FormManagerParams getPreviousMetadata(XMessage message, String formID) {
        String prevPath = null;
        String prevXMl = null;
        FormManagerParams formManagerParams = new FormManagerParams();
        formID = "samagra_workflows"; //TODO: Remove this

        if (!message.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            GupshupStateEntity stateEntity = stateRepo.findByPhoneNoAndBotFormName(message.getTo().getUserID(), formID);
            if (stateEntity != null) {
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
    public XMessage transform(XMessage xMessage) {
        String formID; //= xMessage.getTransformers().get(0).getMetaData().get("Form");
        formID = "samagra_workflows_form_updated_2";
        //formID = "diksha_helper";
        String formPath = getFormPath(formID);

        // Switch from-to
        switchFromTo(xMessage);

        User employee = UserService.findByPhone(xMessage.getTo().getUserID());

        // Get details of user from database
        FormManagerParams previousMeta = getPreviousMetadata(xMessage, formID);


        boolean isApprovalFlow = false;
        // TODO Make a distinction between Form and MenuManager based on Campaign Configuration.
        if (formID.equals("samagra_workflows_form_updated_2")) {

            isApprovalFlow = approvalFlow(employee, xMessage.getPayload().getText(), xMessage);

            if (previousMeta.instanceXMlPrevious == null || previousMeta.currentAnswer.equals("*")) {
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

        ServiceResponse response = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer, previousMeta.instanceXMlPrevious, formPath).start();

        // Create new xMessage from response
        XMessage nextMessage = getMessageFromResponse(xMessage, response);

        // Update database with new fields.
        appendNewResponse(xMessage, response);
        replaceUserState(xMessage, response);

        XMessage cloneMessage = getClone(nextMessage);

        if (response.getCurrentIndex().equals("endOfForm") || response.currentIndex.contains("eof")){
            new UploadService().submit(response.currentResponseState, restTemplate, customRestTemplate);
        }

        if (formID.equals("samagra_workflows_form_updated_2") && !isApprovalFlow && !previousMeta.currentAnswer.equals("#")) {
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
                } else if (response.currentIndex.contains("eof_train_ticket_applied_message_two_way")) {
                } else if (response.currentIndex.contains("eof_train_ticket_cancellation_note")) {
                } else if (response.currentIndex.contains("eof_train_missed_note")) {
                } else {
                }
            }
        }
        return nextMessage;
    }

    private void sendMessageToManagerForApproval(User employee, XMessage nextMessage, SamagraOrgForm orgForm, User manager) {
        String getLeaveMessage = TemplateService.getTemplate(manager.fullName, employee.fullName,
                orgForm.getStartDate(), orgForm.getEndDate(), orgForm.getNumberOfWorkingDays(), orgForm.getReason());
        XMessagePayload payload = XMessagePayload.builder().text(getLeaveMessage).build();
        nextMessage.setPayload(payload);
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
            User admin = UserService.getUserByFullName("Sanchita Dasgupta");
            if (admin != null) {
                String missedFlightMessage = TemplateService.getMissedFlightMessage(employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
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
            User admin = UserService.getUserByFullName("Sanchita Dasgupta");
            if (admin != null) {
                String missedFlightMessage = TemplateService.getTicketCancellationMesssage(employee.fullName, orgForm.getMissedFlightPNR());
                switchFromTo(message);
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
            User admin = UserService.getUserByFullName("Sanchita Dasgupta");
            if (admin != null) {
                String travelDate = (String) orgForm.getAirOneWayData().get("start_date");
                String startCity = (String) orgForm.getAirOneWayData().get("start_city");
                String destinationCity = (String) orgForm.getAirOneWayData().get("destination_city");
                String flightNumber = (String) orgForm.getAirOneWayData().get("enter_onward_flight");
                String oneWayTripMessage = TemplateService.getOneWayTripMessage(employee.fullName,
                        travelDate, startCity, destinationCity, flightNumber);
                switchFromTo(message);
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
            User admin = UserService.getUserByFullName("Sanchita Dasgupta");
            if (admin != null) {
                String travelDate = (String) orgForm.getAirOneWayData().get("start_date");
                String returnDate = (String) orgForm.getAirTwoWayData().get("end_date");
                String startCity = (String) orgForm.getAirOneWayData().get("start_city");
                String destinationCity = (String) orgForm.getAirOneWayData().get("destination_city");
                String flightNumber = (String) orgForm.getAirOneWayData().get("enter_onward_flight");
                String returnFlightNumber = (String) orgForm.getAirTwoWayData().get("enter_return_flight");
                String twoWayTripMessage = TemplateService.getTwoWayTripMessage(employee.fullName,
                        travelDate, returnDate, startCity, destinationCity, flightNumber, returnFlightNumber);
                switchFromTo(message);
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
            String message = getLastSentMessage(manager).getXMessage();
            if (message.contains("1. Approve")) {
                respondToManager(manager, xmsg);
                isApprovalFlow = true;
                // Get the employee for that manager.
                String employeeName = message.split(" from your team has applied for a leave from")[0].split("this is to inform you that ")[1];
                String startDateString = message.split("leave from ")[1].split(" to ")[0];
                String endDateString = message.split("leave from ")[1].split(" to ")[1].split(" for ")[0];
                String workingDays = message.split(" working days ")[0].split(" for ")[2];

                try {
                    User employee = UserService.getUserByFullName(employeeName);
                    boolean isApproved = payload.equals("1");
                    if (isApproved) { // If approved => update leave status; update the end user;
                        LeaveManager.builder().user(employee).build().updateLeaves(Integer.parseInt(workingDays));
                        buildApprovalMessage(employee, xmsg);
                        buildProgramOwnerMessage(employee, xmsg, startDateString, endDateString, workingDays);
                        deleteLastMessage(manager);
                    } else {// If rejected => update the end user;
                        buildRejectionMessage(employee, xmsg);
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

    private void respondToManager(User manager, XMessage message) {
        String approvalMessage = TemplateService.getManagerAcknowledgementMessage(manager.fullName);
        switchFromTo(message);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    // Move all these to a separate transformer

    private void updateLastSentForm(User user) {
    }

    private void buildRejectionMessage(User user, XMessage message) {
        String approvalMessage = TemplateService.getApprovalStatusMessage(user.fullName, "Rejected", (String) user.data.get("reportingManager"));
        switchFromTo(message);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private void buildProgramOwnerMessage(User user, XMessage message, String startDate, String endDate, String numberOfdays) {
        User owner = UserService.getEngagementOwner(user);
        if (owner != null) {
            String approvalMessage = TemplateService.getPOReportMessage(owner.fullName, user.fullName,
                    (String) user.data.get("engagement"), startDate, endDate, numberOfdays);
            switchFromTo(message);
            message.getTo().setUserID(owner.mobilePhone);
            message.getPayload().setText(approvalMessage);
            sendSingle(message);
        }
    }

    private void buildApprovalMessage(User user, XMessage message) {
        String approvalMessage = TemplateService.getApprovalStatusMessage(user.fullName, "Approved", (String) user.data.get("reportingManager"));
        switchFromTo(message);
        message.getTo().setUserID(user.mobilePhone);
        message.getPayload().setText(approvalMessage);
        sendSingle(message);
    }

    private XMessageDAO getLastSentMessage(User user) {
        String url = String.format("http://localhost:8081/getLastMessage?userID=%s&messageType=REPLIED", user.mobilePhone);
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

    private void appendNewResponse(XMessage xMessage, ServiceResponse response) {
        GupshupMessageEntity msgEntity = new GupshupMessageEntity();
        msgEntity.setPhoneNo(xMessage.getTo().getUserID());
        msgEntity.setMessage(xMessage.getPayload().getText());
        msgEntity.setLastResponse(response.getCurrentIndex().equals("endOfForm"));
        msgRepo.save(msgEntity);
    }

    private void replaceUserState(XMessage xMessage, ServiceResponse response) {
        String botFormName = "samagra_workflows"; // = xMessage.getTransformers().get(0).getMetaData().get("Form");
        //String botFormName = "diksha_helper";
        GupshupStateEntity saveEntity = stateRepo.findByPhoneNoAndBotFormName(xMessage.getTo().getUserID(), botFormName);
        if (saveEntity == null) {
            saveEntity = new GupshupStateEntity();
        }
        saveEntity.setPhoneNo(xMessage.getTo().getUserID());
        saveEntity.setPreviousPath(response.getCurrentIndex());
        saveEntity.setXmlPrevious(response.getCurrentResponseState());
        saveEntity.setBotFormName(botFormName);
        stateRepo.save(saveEntity);
    }

}
