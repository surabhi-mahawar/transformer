package com.uci.transformer.odk.entity.converters;

import com.uci.transformer.odk.entity.Assessment;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.SettableValue;

import java.time.LocalDateTime;
import java.util.UUID;

@WritingConverter
public class AssessmentWriteConverter implements Converter<Assessment, OutboundRow> {

    @Override
    public OutboundRow convert(Assessment assessment) {

        LocalDateTime createdOn;
        if(assessment.getCreatedOn() != null) createdOn = assessment.getCreatedOn();
        else createdOn = LocalDateTime.now();

        LocalDateTime updatedOn = LocalDateTime.now();

        OutboundRow row = new OutboundRow();
        row.put("id", SettableValue.from(java.util.UUID.randomUUID()));
        row.put("question", SettableValue.from(assessment.getQuestion().getId()));
        row.put("answer", SettableValue.fromOrEmpty(assessment.getAnswer(), String.class));
        row.put("bot_id", SettableValue.fromOrEmpty(assessment.getBotID(), UUID.class));
        row.put("user_id", SettableValue.fromOrEmpty(assessment.getUserID(), UUID.class));
        row.put("device_id", SettableValue.fromOrEmpty(assessment.getDeviceID(), UUID.class));
        row.put("meta", SettableValue.fromOrEmpty(assessment.getMeta(), Json.class));
        row.put("updated", SettableValue.from(updatedOn));
        row.put("created", SettableValue.from(createdOn));
        return row;
    }
}