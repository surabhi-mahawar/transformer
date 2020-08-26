package com.samagra.transformer.odk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.odk.entity.GupshupMessageEntity;
import com.samagra.transformer.odk.entity.GupshupStateEntity;
import com.samagra.transformer.odk.persistance.FormsDao;
import com.samagra.transformer.odk.persistance.JsonDB;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.samagra.transformer.publisher.CommonProducer;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class ODKTransformer extends TransformerProvider {

    private static final String SMS_BROADCAST_IDENTIFIER = "Broadcast";
    @Autowired
    public XMessageRepo xmsgRepo;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private MessageRepository msgRepo;

    @KafkaListener(id = "transformer1", topics = "Form2")
    public void consumeMessage(String message) throws Exception {
        long startTime = System.nanoTime();
        log.info("Form Transormer Message: " + message);
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        XMessage transformedMessage = this.transform(xMessage);
        kafkaProducer.send("outbound", transformedMessage.toXML());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        log.error("Total time spent in processing form: " + duration/1000000);
    }

    // Listen to topic "Forms"

    // Gets the message => Calls transform() =>  Calls xMessage.completeTransform() =>  send it to inbound-unprocessed


    private FormManagerParams getPreviousMetadata(XMessage message, String formID) {
        String prevPath = null;
        String prevXMl = null;
        FormManagerParams formManagerParams = new FormManagerParams();
        //formID = "samagra_workflows"; //TODO: Remove this


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
        }else{
            formManagerParams.setCurrentAnswer("");
        }

        formManagerParams.setPreviousPath(prevPath);
        formManagerParams.setInstanceXMlPrevious(prevXMl);

        return formManagerParams;
    }


    @Override
    public XMessage transform(XMessage xMessage) {
        String formID; //= xMessage.getTransformers().get(0).getMetaData().get("Form");
        //formID = "samagra_workflows_form";
        formID = "diksha_test_v2";
        String formPath = getFormPath(formID);

        // Switch from-to
        SenderReceiverInfo from = xMessage.getFrom();
        SenderReceiverInfo to = xMessage.getTo();
        xMessage.setFrom(to);
        xMessage.setTo(from);

        // Get details of user from database
        FormManagerParams previousMeta = getPreviousMetadata(xMessage, formID);

        // TODO Make a distinction between Form and MenuManager based on Campaign Configuration.
        ServiceResponse response = new MenuManager(previousMeta.previousPath, previousMeta.currentAnswer, previousMeta.instanceXMlPrevious, formPath).start();

        // Create new xMessage from response
        XMessage nextMessage = getMessageFromResponse(xMessage, response);

        // Update database with new fields.
        appendNewResponse(xMessage, response);
        replaceUserState(xMessage, response);

        return nextMessage;
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
        //String botFormName = "samagra_workflows"; // = xMessage.getTransformers().get(0).getMetaData().get("Form");
        String botFormName ="diksha_test_v2";
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
