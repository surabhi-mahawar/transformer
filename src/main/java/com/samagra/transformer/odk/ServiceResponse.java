package com.samagra.transformer.odk;

public class ServiceResponse {
    String currentIndex;
    String nextMessage; // Next question message
    String currentResponseState;
    boolean shouldSwitchToNextForm;

    public ServiceResponse(String currentIndex, String nextMessage, String currentResponseState) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
    }

    public ServiceResponse(String currentIndex, String nextMessage, String currentResponseState, boolean shouldSwitchToNextForm) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
        this.shouldSwitchToNextForm = shouldSwitchToNextForm;
    }

    public String getCurrentIndex() {
        return currentIndex;
    }

    public String getNextMessage() {
        return nextMessage;
    }

    public String getCurrentResponseState() {
        return currentResponseState;
    }
}
