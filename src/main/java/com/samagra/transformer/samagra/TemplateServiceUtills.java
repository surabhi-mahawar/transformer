package com.samagra.transformer.samagra;

import java.io.StringReader;
import java.util.HashMap;

public class TemplateServiceUtills {

    private static HashMap<String,String> map = new HashMap<>();

    static{
        map.put("LeaveMessage","Hi %s, this is to inform you that %s from your team has applied for a %s " +
                "leave from %s to %s for %s working days with reason being %s. \n" +
                "Please choose one of the following options:\n\n 1. Approve \n 2. Reject \n");
        map.put("ApprovalStatus","Hi %s! This is to inform you that your leave has been %s by your manager."
                +"Your program owner %s has also been apprised about your leave. Thanks!");
        map.put("RejectionStatusMessage","Hi %s! This is to inform you that your leave has been %s by your manager.");
        map.put("ManagerAcknowledgementMessage","Thanks %s! Your action has been recorded. The team member and program owner will receive the relevant update.");
        map.put("POReportMessage","Hi %s, this is to inform you that %s from team %s will be on leave from %s to %s for %s working days, as approved by the manager.");
        map.put("OneWayTripMessage","Hi Sanchita! %s has submitted an air travel request for %s from %s to %s for %s.");
        map.put("TwoWayTripMessage","Hi Sanchita! %s has submitted an air travel request for %s to %s from %s to %s on %s and %s.");
        map.put("TicketCancellationMesssage","Hi Sanchita! %s has submitted a cancellation request for %s.");
        map.put("MissedFlightMessage","Hi Sanchita! Kindly note that %s was unsuccessful in boarding the flight booked under %s.");
        map.put("OneWayTrainTicketMessage","Hi Raju! %s has submitted an train travel request for %s from %s to %s for %s.");
        map.put("TwoWayTrainTicketMessage","Hi Raju! %s has submitted an train travel request for %s to %s from %s to %s on %s and %s.");
        map.put("TrainTicketCancellationMessage","Hi Raju! %s has submitted a cancellation request for %s.");
        map.put("TrainMissedMessage","Kindly note that %s was unsuccessful in boarding the train booked under %s.");
    }

    public static String getFormattedString(String templateKey, String... arg) throws Exception {
        String rv =  String.format(map.get(templateKey),arg);
        return rv;
    }

    public static String[] getVariablesFromTemplateMessage(String message)throws  Exception{
        String[] strArray =null;
        for(String formatt : map.values()) {
            try {
                Object[] var = new FormatReader(new StringReader(message)).scanf(formatt);
                    strArray = new String[var.length];
                for (int i = 0; i < var.length; i++)
                    strArray[i] = String.valueOf(var[i]);
                return strArray;
            } catch(Exception e){
            }
        }
        return strArray;
    }
}
