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

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
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

            List<String> users = UserService.findUsersForESamwad(CampaignService.getCampaignFromName(xMessage.getApp()).name);
            for (String user : users) {
                userIds.append(user).append(",");
            }
            if (userIds != null && userIds.length() > 0) {
                ids = userIds.substring(0, userIds.length() - 1);
            }
            SenderReceiverInfo to = xMessage.getTo();
            to.setUserID(ids);
            xMessage.setTo(to);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in getting users for this campaign");
        }
        return xMessage;
    }

    private XMessage getClone(XMessage nextMessage) {
        XMessage cloneMessage = null;
        try {
            cloneMessage = XMessageParser.parse(new ByteArrayInputStream(nextMessage.toXML().getBytes()));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return cloneMessage;
    }

    @Override
    public List<XMessage> transformToMany(XMessage xMessage) {
        ArrayList<XMessage> messages = new ArrayList<>();

        ArrayList<String> groups = xMessage.getTo().getGroups();
        for (String group : groups) {
            try {
                List<User> users = UserService.findUsersForGroup(group);
                for (User user : users) {
                    XMessage clone = getClone(xMessage);

                    SenderReceiverInfo to = clone.getTo();
                    to.setUserID(user.mobilePhone);
                    clone.setTo(to);

                    clone.setMessageState(XMessage.MessageState.NOT_SENT);

                    messages.add(clone);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return messages;

    }

    @KafkaListener(id = "BroadcastTransformer", topics = "BroadcastTransformer")
    public void consumeMessage(String message) throws Exception {

        long startTime = System.nanoTime();
        log.info("BroadcastTransformer Transormer Message: " + message);

        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        String userServer = xMessage.getTo().getMeta().get("userServer");

        if (userServer.equals("") || userServer.equals("eSamwad")) {
            XMessage transformedMessage = this.transform(xMessage);
            kafkaProducer.send("outbound", transformedMessage.toXML());
        } else if (userServer.equals("AuthServer")) {
            ArrayList<XMessage> messages = (ArrayList<XMessage>) this.transformToMany(xMessage);
            for (XMessage msg : messages) {
                kafkaProducer.send("outbound", msg.toXML());
            }

        } else {
            log.error("No suitable User Repository found");
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        log.error("Total time spent in processing form: " + duration / 1000000);
    }
}
