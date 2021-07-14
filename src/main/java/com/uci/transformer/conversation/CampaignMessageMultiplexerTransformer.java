package com.uci.transformer.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.transformer.TransformerProvider;
import com.uci.utils.kafka.SimpleProducer;
import io.fusionauth.domain.Application;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import messagerosa.xml.XMessageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@Slf4j
@Component
public class CampaignMessageMultiplexerTransformer extends TransformerProvider {

    @Autowired
    public SimpleProducer kafkaProducer;

    // Not getting used in this one
    @Override
    public Mono<XMessage> transform(XMessage xMessage) {
        return null;
    }

    @Override
    public Mono<List<XMessage>> transformToMany(XMessage parentXMessage) {
        Application campaign;
        List<XMessage> startingMessagesForUsers = new ArrayList<>();
        //Fixme !!Important
        /*
        try {
            campaign = CampaignService.getCampaignFromName(parentXMessage.getApp());
            List<User> users = UserService.findUsersForCampaign(parentXMessage.getApp());
            for (User user : users) {
                if (user.getRoleNamesForApplication(campaign.id) != null && !user.getRoleNamesForApplication(campaign.id).contains("Admin")) {
                    SenderReceiverInfo to = SenderReceiverInfo
                            .builder()
                            .userID("admin")
                            .build(); //Admin for the campaign

                    SenderReceiverInfo from = SenderReceiverInfo
                            .builder()
                            .userID(user.mobilePhone)
                            .formID(parentXMessage.getTransformers().get(0).getMetaData().get("Form"))
                            .build(); //user of the campaign

                    ConversationStage conversationStage = ConversationStage
                            .builder()
                            .state(ConversationStage.State.STARTING)
                            .build();

                    XMessageThread thread = XMessageThread.builder()
                            .offset(0)
                            .startDate(new Date().toString())
                            .lastMessageId("0")
                            .build();

                    XMessage userMessage = XMessage.builder()
                            .transformers(parentXMessage.getTransformers())
                            .from(from)
                            .to(to)
                            .messageState(XMessage.MessageState.NOT_SENT)
                            .channelURI(parentXMessage.getChannelURI())
                            .providerURI(parentXMessage.getProviderURI())
                            .timestamp(System.currentTimeMillis())
                            .app(campaign.name)
                            .thread(thread)
                            .conversationStage(conversationStage)
                            .build();

                    startingMessagesForUsers.add(userMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        return Mono.just(startingMessagesForUsers);
    }

    @KafkaListener(id = "transformer-campaign-multiplexer", topics = "CampaignMessageMultiplexer")
    public void consumeMessage(String message) throws Exception {
        log.info("CampaignMessageMultiplexer Transformer Message: " + message);
        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
                this.transformToMany(xMessage).subscribe(new Consumer<List<XMessage>>() {
                    @Override
                    public void accept(List<XMessage> transformedMessages) {

                        for (XMessage msg : transformedMessages) {
                            try {
                                kafkaProducer.send("Form2", msg.toXML());
                            } catch (JAXBException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

    }

}
