package com.samagra.transformer.application;


import com.samagra.transformer.odk.MenuManager;
import com.samagra.transformer.odk.ODKTransformer;
import com.samagra.transformer.odk.ServiceResponse;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicLong;

@Log
@RestController
public class FormTransformerTestAPI {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @SneakyThrows
    @GetMapping("/generate-message")
    public ServiceResponse greeting(@RequestParam(value = "previousPath", required = false) String previousPath,
                                    @RequestParam(value = "currentAnswer", required = false) String currentAnswer,
                                    @RequestParam(value = "instanceXMlPrevious", required = false) String instanceXMlPrevious,
                                    @RequestParam(value = "botFormName", required = false) String botFormName) {

        if(previousPath != null) previousPath = URLDecoder.decode(previousPath, "UTF-8");
        if(instanceXMlPrevious != null) instanceXMlPrevious = URLDecoder.decode(instanceXMlPrevious, "UTF-8");
        if(currentAnswer != null) currentAnswer = URLDecoder.decode(currentAnswer, "UTF-8");
        log.info("PreviousPath: " + previousPath);
        log.info("CurrentAnswer" +  currentAnswer);
        log.info("InstanceCurrentXML" + instanceXMlPrevious);
        log.info("botFormName" +  botFormName);
        String formPath = ODKTransformer.getFormPath(botFormName);
        ServiceResponse serviceResponse = new MenuManager(previousPath, currentAnswer, instanceXMlPrevious, formPath).start();
        System.out.println(serviceResponse.getCurrentResponseState());
        return serviceResponse;
    }
}
