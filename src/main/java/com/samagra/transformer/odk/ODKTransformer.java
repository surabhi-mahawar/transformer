package com.samagra.transformer.odk;

import com.samagra.transformer.TransformerProvider;


public class ODKTransformer extends TransformerProvider {

    // Listen to topic "Forms"

    // Gets the message => Calls transform() =>  Calls xMessage.completeTransform() =>  send it to inbound-unprocessed

    @Override
    public void transform() {
        return;
    }
}
