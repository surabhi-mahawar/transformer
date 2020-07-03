package com.samagra.transformer;

import lombok.*;
import messagerosa.core.model.XMessage;

@NoArgsConstructor
public abstract class TransformerProvider {
    int id;
    String name;

    String description;

    XMessage initialState;
    XMessage finalState;


    public abstract XMessage transform(XMessage xMessage);

}
