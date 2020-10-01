package com.samagra.transformer;

import com.samagra.transformer.User.CampaignService;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.publisher.CommonProducer;
import io.fusionauth.domain.User;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Component
public class BroadcastTransformer extends TransformerProvider {

    @Autowired
    public XMessageRepo xmsgRepo;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private MessageRepository msgRepo;

    @Override
    public XMessage transform(XMessage xMessage) {
        //TODO: Get all the phone numbers for the users and update the userID

        try {
            StringBuilder userIds = new StringBuilder();
            String ids = "";
            List<User> users = UserService.findUsersForCampaign(CampaignService.getCampaignFromName(xMessage.getApp()).name);
            for(User user: users){
                userIds.append(user.mobilePhone).append(",");
            }
            if (userIds != null && userIds.length() > 0) {
                ids = userIds.substring(0, userIds.length() - 1);
            }
            SenderReceiverInfo to = xMessage.getTo();
            to.setUserID(ids);
            xMessage.setTo(to);
        }catch (Exception e){

        }
        return xMessage;
    }

    @Override
    public List<XMessage> transformToMany(XMessage xMessage) {
        return null;
    }

    @KafkaListener(id = "BroadcastTransformer", topics = "BroadcastTransformer")
    public void consumeMessage(String message) throws Exception {
        long startTime = System.nanoTime();
        log.info("BroadcastTransformer Transormer Message: " + message);
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        XMessage transformedMessage = this.transform(xMessage);
        kafkaProducer.send("outbound", transformedMessage.toXML());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        log.error("Total time spent in processing form: " + duration / 1000000);
    }
}
