package com.samagra.transformer;

import com.samagra.transformer.User.CampaignService;
import com.samagra.transformer.User.UserService;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.publisher.CommonProducer;
import io.fusionauth.domain.Application;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired
    private UserService userService;

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
        try{
            Application currentApplication = CampaignService.getCampaignFromNameESamwad(xMessage.getApp());
            ArrayList<HashMap<String, Object>> d = (ArrayList) currentApplication.data.get("parts");
            if(d.get(0).get("template") != null){
                ArrayList<UserWithTemplate> usersWithTemplate = userService.getUsersAndTemplateFromFederatedServers(xMessage.getApp());
                for(UserWithTemplate u: usersWithTemplate){
                    XMessage clone = getClone(xMessage);
                    XMessagePayload payload = clone.getPayload();
                    payload.setText(u.getText().replace("\\n", "\n"));
                    clone.setPayload(payload);

                    SenderReceiverInfo to = clone.getTo();
                    to.setUserID(u.getUserPhone());
                    clone.setTo(to);

                    clone.setMessageState(XMessage.MessageState.NOT_SENT);
                    messages.add(clone);
                }
            }else{
                List<String> users = UserService.getUsersFromFederatedServers(xMessage.getApp());
                System.out.println("Total users:: " + users.size());
                for (String phone: users){
                    XMessage clone = getClone(xMessage);

                    SenderReceiverInfo to = clone.getTo();
                    to.setUserID(phone);
                    clone.setTo(to);

                    clone.setMessageState(XMessage.MessageState.NOT_SENT);
                    messages.add(clone);
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }

        return messages;

    }

    @KafkaListener(id = "BroadcastTransformer", topics = "BroadcastTransformer")
    public void consumeMessage(String message) throws Exception {

        long startTime = System.nanoTime();
        log.info("BroadcastTransformer Transformer Message: " + message);

        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
        ArrayList<XMessage> messages = (ArrayList<XMessage>) this.transformToMany(xMessage);
        for (XMessage msg : messages) {
            kafkaProducer.send("broadcast", msg.toXML());
            kafkaProducer.send("PassThrough", msg.toXML());
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        log.error("Total time spent in processing form: " + duration / 1000000);
    }
}
