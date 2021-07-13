package com.uci.transformer.odk.entity;

import lombok.*;
import messagerosa.core.model.ButtonChoice;
import messagerosa.core.model.ContactCard;
import messagerosa.core.model.LocationParams;
import messagerosa.core.model.MessageMedia;

import java.io.Serializable;
import java.util.ArrayList;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meta implements Serializable {

    private String title;
    private ArrayList<String> choices;

    @Override
    public String toString() {
        return "";
    }
}