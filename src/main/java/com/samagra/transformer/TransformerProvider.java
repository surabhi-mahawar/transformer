package com.samagra.transformer;

import lombok.*;
import messagerosa.core.model.XMessage;

import java.util.List;

@NoArgsConstructor
public abstract class TransformerProvider {
    int id;
    String name;

    String description;

    XMessage initialState;
    XMessage finalState;


    public abstract XMessage transform(XMessage xMessage) throws Exception;

    public abstract List<XMessage> transformToMany(XMessage xMessage);

}


