package com.samagra.transformer.samagra;

import com.samagra.transformer.User.UserService;
import io.fusionauth.domain.User;
import lombok.Builder;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Calendar;
import java.util.Date;

@Builder
public class LeaveManager {
    User user;

    public int getCurrentLeaves(){
        return (int) user.data.get("currentLeaves");
    }

    public double accruedLeavesSinceDateOfJoining(){
        return 0.0;
    }


    public int getCurrentFiscalYear(){
        int CurrentYear = Calendar.getInstance().get(Calendar.YEAR);
        int CurrentMonth = (Calendar.getInstance().get(Calendar.MONTH)+1);
        if (CurrentMonth<4) return CurrentYear-1;
        else return CurrentYear;
    }

    public double accruedLeavesForCurrentYear(){
        double ACCRUAL_CONST = 18/365.0;
        DateTime startOfFinancialYear = new DateTime(getCurrentFiscalYear(), 4, 1, 0, 0, 0, 0);
        int daysInThisYear = Days.daysBetween(startOfFinancialYear, new DateTime()).getDays();
        return ACCRUAL_CONST*daysInThisYear;
    }

    public double accruedLeavedSinceDate(String date){
        double ACCRUAL_CONST = 18/365.0;
        int day = Integer.parseInt(date.split("-")[0]);
        int month = Integer.parseInt(date.split("-")[1]);
        int year = Integer.parseInt(date.split("-")[2]);
        DateTime startOfFinancialYear = new DateTime(year, month, day, 0,0,0,0);
        int daysInThisYear = Days.daysBetween(new DateTime(startOfFinancialYear), new DateTime()).getDays();
        return ACCRUAL_CONST*daysInThisYear;
    }

    public void updateLeaves(int workingDays){

        double accruedLeaves = 0.0;
        double previousLeaves = 0.0;
        if(user.data.get("lastUpdatedAt") != null){
            accruedLeaves = accruedLeavedSinceDate((String) user.data.get("lastUpdatedAt"));
            previousLeaves = (double) user.data.get("accurateLeaves");
        }else{
            accruedLeaves = accruedLeavesForCurrentYear();
            previousLeaves = Double.parseDouble(user.data.get("leavesAvailable").toString());
        }


        DateTime dt = DateTime.now();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyyy");
        user.data.put("lastUpdatedAt", fmt.print(dt));
        user.data.put("accurateLeaves", accruedLeaves+previousLeaves - workingDays);
        user.data.put("leavesAvailable", Math.round(accruedLeaves+previousLeaves) - workingDays);

        UserService.update(user);

    }
}
