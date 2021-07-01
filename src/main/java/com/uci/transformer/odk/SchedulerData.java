package com.uci.transformer.odk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SchedulerData implements Serializable {
    String employeeName;
    String teamName;
    String startDateString;
    String status;
}
