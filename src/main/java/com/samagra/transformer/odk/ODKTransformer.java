package com.samagra.transformer.odk;

import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.odk.entity.GupshupStateEntity;
import com.samagra.transformer.odk.repository.StateRepository;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.samagra.transformer.publisher.CommonProducer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

@Slf4j
@Component
public class ODKTransformer extends TransformerProvider {

    ArrayList<FormAttributes> formAttributes = new ArrayList<>();

    private static final String SMS_BROADCAST_IDENTIFIER = "Broadcast";
    @Autowired
    public XMessageRepo xmsgRepo;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    private StateRepository stateRepo;

    @KafkaListener(id = "transformer", topics = "Form")
    public void consumeMessage(String message) throws Exception {
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        XMessage transformedMessage = this.transform(xMessage);
        kafkaProducer.send("outbound", transformedMessage.toXML());
    }

    public void setup(){
        formAttributes.add(new FormAttributes().builder().id("rozgar_portal_survey_1").name("Rozgar Portal Survey").path("/downloads/Rozgar_Portal_Survey.xml").build());
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
        if(message.getPayload().getText() == null){
            formManagerParams.setCurrentAnswer(message.getPayload().getMedia().getUrl());
        }else formManagerParams.setCurrentAnswer(message.getPayload().getText());

        formManagerParams.setPreviousPath(prevPath);
        formManagerParams.setInstanceXMlPrevious(prevXMl);

        return formManagerParams;
    }


    @Override
    public XMessage transform(XMessage xMessage) {
        setup();
        String formID = xMessage.getTransformers().get(0).getMetaData().get("Form");
        String formPath = getFormPath(formID);
        formPath = System.getProperty("user.dir") + formPath;

        // Get details of User from database

        FormManagerParams previousMeta = getPreviousMetadata(xMessage);
        ServiceResponse response = new FormManager(previousMeta.previousPath, previousMeta.currentAnswer,
                previousMeta.instanceXMlPrevious, formPath).start();

        // Create new xMessage from response
        XMessage nextMessage = getMessageFromResponse(response, xMessage);

        // Update database with new fields.
        return nextMessage;
    }

    private XMessage getMessageFromResponse(ServiceResponse response, XMessage xMessage) {
        return null;
    }

    private String getFormPath(String formID) {
        for(FormAttributes formAttributes : this.formAttributes){
            if(formAttributes.id.equals(formID)) return formAttributes.path;
        }
        return null;
    }
}
