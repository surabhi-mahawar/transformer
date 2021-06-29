package com.samagra.transformer.odk;

import messagerosa.core.model.XMessagePayload;

public class ServiceResponse {
    String currentIndex;
    XMessagePayload nextMessage; // Next question message
    String currentResponseState;
    boolean shouldSwitchToNextForm;

    public ServiceResponse(String currentIndex, XMessagePayload nextMessage, String currentResponseState) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
    }

    public ServiceResponse(String currentIndex, XMessagePayload nextMessage, String currentResponseState, boolean shouldSwitchToNextForm) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
        this.shouldSwitchToNextForm = shouldSwitchToNextForm;
    }

    public String getCurrentIndex() {
        return currentIndex;
    }

    public XMessagePayload getNextMessage() {
        return nextMessage;
    }

    public String getCurrentResponseState() {
        return currentResponseState;
    }
}
