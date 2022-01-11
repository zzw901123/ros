/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.util.time;

/**
 *
 * @author zzw
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class TimeZoneFormat {
    
    public static String getFormatedDateString(int timeZoneOffset, int offsetTime) {
        if (timeZoneOffset > 14 || timeZoneOffset < -12) {
            System.out.println("Configuration item TimeZone " + timeZoneOffset + " is invalid.");
            timeZoneOffset = 0;
        }
        
        TimeZone timeZone;
        
        String[] ids = TimeZone.getAvailableIDs(timeZoneOffset * 60 * 60 * 1000);
        
        if (ids.length == 0) {
            // Use default TimeZone
            timeZone = TimeZone.getDefault();
        } else {
            timeZone = new SimpleTimeZone(timeZoneOffset * 60 * 60 * 1000, ids[0]);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        simpleDateFormat.setTimeZone(timeZone);
        
        // Delay time
        long currentTime = System.currentTimeMillis();
        currentTime += offsetTime;
        // Change +0800
        return simpleDateFormat.format(new Date(currentTime)).replaceAll("\\+0800", "z");
    }
    
}


