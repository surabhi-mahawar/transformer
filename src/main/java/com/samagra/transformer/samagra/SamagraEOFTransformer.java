package com.samagra.transformer.samagra;

import com.samagra.transformer.TransformerProvider;
import com.samagra.transformer.odk.repository.MessageRepository;
import com.samagra.transformer.odk.repository.StateRepository;
import com.samagra.transformer.publisher.CommonProducer;
import com.uci.dao.repository.XMessageRepository;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class SamagraEOFTransformer extends TransformerProvider {

    @Autowired
    public XMessageRepository xmsgRepo;

    @Autowired
    public CommonProducer kafkaProducer;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private MessageRepository msgRepo;

    @Override
    public Mono<XMessage> transform(XMessage xMessage) {
        return null;
    }

    @Override
    public Mono<List<XMessage>> transformToMany(XMessage xMessage) {
        return null;
    }

//    @KafkaListener(id = "SamagraEOFTransformer", topics = "SamagraEOFTransformer")
//    public void consumeMessage(String message) throws Exception {
//        long startTime = System.nanoTime();
//        log.info("SamagraEOFTransformer Transormer Message: " + message);
//        XMessage xMessage = XMessageParser.parse(new ByteArrayInputStream(message.getBytes()));
//        XMessage transformedMessage = this.transform(xMessage);
//        kafkaProducer.send("outbound", transformedMessage.toXML());
//        long endTime = System.nanoTime();
//        long duration = (endTime - startTime);
//        log.error("Total time spent in processing form: " + duration/1000000);
//    }
}
