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
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.samagra.transformer.publisher.CommonProducer;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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

    @KafkaListener(id = "transformer", topics = "Form")
    public void consumeMessage(String message) throws Exception {
        log.info("Form Transormer Message: " + message);
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        XMessage transformedMessage = this.transform(xMessage);
        kafkaProducer.send("outbound", transformedMessage.toXML());
    }

    // Listen to topic "Forms"

    // Gets the message => Calls transform() =>  Calls xMessage.completeTransform() =>  send it to inbound-unprocessed


    private FormManagerParams getPreviousMetadata(XMessage message) {
        String prevPath = null;
        String prevXMl = null;

        GupshupStateEntity stateEntity = stateRepo.findByPhoneNo(message.getFrom().getUserID());
        if (stateEntity != null) {
            prevXMl = stateEntity.getXmlPrevious();
            prevPath = stateEntity.getPreviousPath();
        }

        FormManagerParams formManagerParams = new FormManagerParams();

        // Handle image responses to a question
        if (message.getPayload() != null) {
            if (message.getPayload().getText() == null) {
                formManagerParams.setCurrentAnswer(message.getPayload().getMedia().getUrl());
            } else formManagerParams.setCurrentAnswer(message.getPayload().getText());
        } else {
            formManagerParams.setCurrentAnswer(null);
        }

        formManagerParams.setPreviousPath(prevPath);
        formManagerParams.setInstanceXMlPrevious(prevXMl);

        return formManagerParams;
    }


    @Override
    public XMessage transform(XMessage xMessage) {
        String formID = xMessage.getTransformers().get(0).getMetaData().get("Form");
        formID = "resume_questionnaire_v3";
        String formPath = getFormPath(formID);

        // Get details of User from database

        FormManagerParams previousMeta = getPreviousMetadata(xMessage);
        ServiceResponse response = new FormManager(previousMeta.previousPath, previousMeta.currentAnswer,
                previousMeta.instanceXMlPrevious, formPath).start();

        // Create new xMessage from response
        XMessage nextMessage = getMessageFromResponse(xMessage, response);

        // Update database with new fields.
        try {
            appendNewResponse(xMessage, response);
            replaceUserState(xMessage, response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

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

    private String getFormPath(String formID) {
        try {
            FormsDao dao = new FormsDao(JsonDB.setupDatabase());
            return dao.getFormsCursorForFormId(formID).getFormFilePath();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void appendNewResponse(XMessage xMessage, ServiceResponse response) throws JsonProcessingException {
        GupshupMessageEntity msgEntity = msgRepo.findByPhoneNo(xMessage.getTo().getUserID());

        if (msgEntity == null) {
            msgEntity = new GupshupMessageEntity();
        }
        msgEntity.setPhoneNo(xMessage.getTo().getUserID());
        msgEntity.setMessage(xMessage.getPayload().getText());
        msgEntity.setLastResponse(response.getCurrentIndex().equals("endOfForm"));
        msgRepo.save(msgEntity);
    }

    private void replaceUserState(XMessage xMessage, ServiceResponse response) throws JAXBException {
        GupshupStateEntity saveEntity = stateRepo.findByPhoneNo(xMessage.getTo().getUserID());
        if (saveEntity == null) {
            saveEntity = new GupshupStateEntity();
        }
        saveEntity.setPhoneNo(xMessage.getTo().getUserID());
        saveEntity.setPreviousPath(response.getCurrentIndex());
        saveEntity.setXmlPrevious(response.getCurrentResponseState());
        saveEntity.setBotFormName(null);
        stateRepo.save(saveEntity);
    }
}
