package com.samagra.transformer.odk;

public class ServiceResponse {
    String currentIndex;
    String nextMessage; // Next question message
    String currentResponseState;

    public ServiceResponse(String currentIndex, String nextMessage, String currentResponseState) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
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
