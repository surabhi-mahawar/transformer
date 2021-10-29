package com.uci.transformer.health;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.utils.UtilHealthService;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthController {

	@Autowired
	private UtilHealthService healthService;
	
    @RequestMapping(value = "/health", method = RequestMethod.GET, produces = { "application/json", "text/json" })
    public ResponseEntity<JsonNode> statusCheck() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        /* Current Date Time */
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String dateString = fmt.format(localNow).toString();
        
        JsonNode jsonNode = mapper.readTree("{\"id\":\"api.content.health\",\"ver\":\"3.0\",\"ts\":\"2021-06-26T22:47:05Z+05:30\",\"params\":{\"resmsgid\":\"859fee0c-94d6-4a0d-b786-2025d763b78a\",\"msgid\":null,\"err\":null,\"status\":\"successful\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"checks\":[],\"healthy\":false}}");
        
        JsonNode resultNode = mapper.readTree("{\"checks\":[{\"name\":\"kafka\",\"healthy\":false},{\"name\":\"campaign\",\"healthy\":false}],\"healthy\":true}");
        
        /* Kafka health info */
        JsonNode kafkaHealthNode = healthService.getKafkaHealthNode();
        JsonNode kafkaNode = mapper.createObjectNode();
        ((ObjectNode) kafkaNode).put("name", "Kafka");
        ((ObjectNode) kafkaNode).put("healthy", kafkaHealthNode.get("healthy").asBoolean());
        ((ObjectNode) kafkaNode).put("details", kafkaHealthNode.get("details"));
        
        /* create `ArrayNode` object */
        ArrayNode arrayNode = mapper.createArrayNode();
        
        /* add JSON users to array */
        arrayNode.addAll(Arrays.asList(kafkaNode));
        
        ((ObjectNode) resultNode).putArray("checks").addAll(arrayNode);
        
        /* System overall health */
        if(kafkaHealthNode.get("healthy").booleanValue()) {
        	((ObjectNode) resultNode).put("healthy", true);
        } else {
        	((ObjectNode) resultNode).put("healthy", false);
        }
        
        ((ObjectNode) jsonNode).put("ts", dateString);
        ((ObjectNode) jsonNode).put("result", resultNode);
        
        return ResponseEntity.ok(jsonNode);
    }
}
