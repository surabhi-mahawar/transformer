package com.samagra.transformer.samagra;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TemplateServiceUtills {

    private static HashMap<String,String> map = new HashMap<>();

    static{
        map.put("LeaveMessage","Hi %s, this is to inform you that %s from your team has applied for a %s \n" +
                "                leave from %s to %s for %s working days with reason being %s. \n\n" +
                "                Please choose one of the following options:\n\n 1. Approve \n 2. Reject \n");

        map.put("ApprovalStatus","Hi %s! This is to inform you that your leave has been %s by your manager."
                +"Your program owner %s has also been apprised about your leave. Thanks!");
        map.put("RejectionStatusMessage","Hi %s! This is to inform you that your leave has been %s by your manager.");
        map.put("ManagerAcknowledgementMessage","Thanks %s! Your action has been recorded. The team member and program owner will receive the relevant update.");
        map.put("POReportMessage","Hi %s, this is to inform you that %s from team %s will be on leave from %s to %s for %s working days, as approved by the manager.");
        map.put("OneWayTripMessage","%s has submitted an air travel request for %s from %s to %s for %s");
        map.put("TwoWayTripMessage","%s has submitted an air travel request for %s to %s from %s to %s on %s and %s");
        map.put("TicketCancellationMesssage","Hi Sanchita. %s has submitted a cancellation request for %s");
        map.put("MissedFlightMessage","Kindly note that %s was unsuccessful in boarding the flight booked under %s.");
        map.put("OneWayTrainTicketMessage","%s has submitted an train travel request for %s from %s to %s for %s");
        map.put("TwoWayTrainTicketMessage","%s has submitted an train travel request for %s to %s from %s to %s on %s and %s");
        map.put("TrainTicketCancellationMessage","Hi Raju. %s has submitted a cancellation request for %s.");
        map.put("TrainMissedMessage","Kindly note that %s was unsuccessful in boarding the train booked under %s.");
    }

//    public static void main(String arg[]) throws Exception {
//        getFormattedString("template12","vishal","singla");
//    }
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

//    public static String getLeaveMessageTemplate(String managerName, String employeeName, String reason, String startDate, String endDate, String numberOfDays, String reasonForLeave){
//        String template =  "Hi %s, this is to inform you that %s from your team has applied for a %s " +
//                "leave from %s to %s for %s working days with reason being %s.\n" +
//                "\n" +
//                "Please choose one of the following options:\n" +
//                "1. Approve \n" + "2. Reject \n";
//        return String.format(template, managerName, employeeName, reason, startDate, endDate, numberOfDays, reasonForLeave);
////    }
//    public static String approvalStatus(String employeeName, String status, String ownerName){
//        String template = "Hi %s! This is to inform you that your leave has been %s by your manager."
//                + "Your program owner %s has also been apprised about your leave. Thanks!";
//        return String.format(template, employeeName, status, ownerName);
//    }
//
//    public static String getRejectionStatusMessage(String employeeName, String status, String ownerName){
//        String template = "Hi %s! This is to inform you that your leave has been %s by your manager.";
//        return String.format(template, employeeName, status, ownerName);
//    }
//
//    public static String getManagerAcknowledgementMessage(String managerName){
//        String template = "Thanks %s! Your action has been recorded. The team member and program owner will receive the relevant update.";
//        return String.format(template, managerName);
//    }
//
//    public static String getPOReportMessage(String ownerName, String employeeName, String teamName, String startDate, String endDate, String numberOfDays){
//        String template = "Hi %s, this is to inform you that %s from team %s will be on leave from %s to %s for %s working days, as approved by the manager.";
//        return String.format(template, ownerName, employeeName, teamName, startDate, endDate, numberOfDays);
//    }

//    public static String getOneWayTripMessage(String employeeName, String travelDate, String startCity, String destinationCity, String flightNumber){
//        String template = "%s has submitted an air travel request for %s from %s to %s for %s";
//        return String.format(template, employeeName, travelDate, startCity, destinationCity, flightNumber);
//    }

//    public static String getTwoWayTripMessage(String employeeName, String travelDate, String returnDate, String startCity, String destinationCity, String flightNumber, String returnFlightNumber){
//        String template = "%s has submitted an air travel request for %s to %s from %s to %s on %s and %s";
//        return String.format(template, employeeName, travelDate, returnDate, startCity, destinationCity, flightNumber, returnFlightNumber);
//    }
//
//    public static String getTicketCancellationMesssage(String employeeName, String pnr){
//        String template = "Hi Sanchita. %s has submitted a cancellation request for %s";
//        return String.format(template, employeeName, pnr);
//    }
//
//    public static String getMissedFlightMessage(String employeeName, String pnr){
//        String template = "Kindly note that %s was unsuccessful in boarding the flight booked under %s.";
//        return String.format(template, employeeName, pnr);
////    }
//
//    public static String getOneWayTrainTicketMessage(String employeeName, String travelDate, String startCity, String endCity, String trainNo){
//        String template = "%s has submitted an train travel request for %s from %s to %s for %s";
//        return String.format(template, employeeName, travelDate, startCity, endCity, trainNo);
//    }

//    public static String getTwoWayTrainTicketMessage(String employeeName, String onwardDate, String returnDate, String startCity, String endCity, String onwardTrainNo, String returnTrainNo){
//        String template = "%s has submitted an train travel request for %s to %s from %s to %s on %s and %s";
//        return String.format(template, employeeName, onwardDate, returnDate, startCity, endCity, onwardTrainNo, returnTrainNo);
//    }

//    public static String getTrainTicketCancellationMessage(String employeeName, String pnr){
//        String template = "Hi Raju. %s has submitted a cancellation request for %s.";
//        return String.format(template, employeeName, pnr);
//    }
//
//    public static String getTrainMissedMessage(String employeeName, String pnr){
//        String template = "Kindly note that %s was unsuccessful in boarding the train booked under %s.";
//        return String.format(template, employeeName, pnr);
//    }

}
