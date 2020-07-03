package com.samagra.transformer.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommonProducer {

  private KafkaTemplate<String, String> simpleProducer;


  public CommonProducer(KafkaTemplate<String, String> simpleProducer1) {
    this.simpleProducer = simpleProducer1;
  }

  public void send(String topic, String message) throws JsonProcessingException {
	  log.info("Message being publish to {} topic ",topic);
    log.info("published {}", new ObjectMapper().writeValueAsString(simpleProducer.send(topic, message)));
  }
}
