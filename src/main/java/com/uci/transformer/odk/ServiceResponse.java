package com.uci.transformer.odk;

import com.uci.transformer.odk.entity.Question;
import messagerosa.core.model.XMessagePayload;

public class ServiceResponse {
    String currentIndex;
    XMessagePayload nextMessage; // Next question message
    String currentResponseState;
    boolean shouldSwitchToNextForm;
    String formVersion;
    String formID;
    Question question;

    public ServiceResponse(String currentIndex, XMessagePayload nextMessage, String currentResponseState, String formVersion) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
        this.formVersion = formVersion;
    }

    public ServiceResponse(String currentIndex, XMessagePayload nextMessage, String currentResponseState, boolean shouldSwitchToNextForm, String formVersion) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
        this.shouldSwitchToNextForm = shouldSwitchToNextForm;
        this.formVersion = formVersion;
    }

    public ServiceResponse(String currentIndex, XMessagePayload nextMessage, String currentResponseState, String formVersion, String formID, Question question) {
        this.currentIndex = currentIndex;
        this.nextMessage = nextMessage;
        this.currentResponseState = currentResponseState;
        this.formVersion = formVersion;
        this.formID = formID;
        this.question = question;
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
