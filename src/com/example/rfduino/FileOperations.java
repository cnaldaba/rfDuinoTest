package com.example.rfduino;

//Source: http://www.learn2crack.com/2014/04/android-read-write-file.html

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;

public class FileOperations {
   
   private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CANADA);
   private final int sampleRate = 300;
  
   
   public FileOperations() {
      }
   
   
   public void writeHeader(String fname, String fPath, String userName, String date, String time){
	      try {
	          
	          File file = new File(fPath, fname);
	          // If file does not exists, then create it
	          if (!file.exists()) {
	            file.createNewFile();
	          }
	          FileWriter fw = new FileWriter(file.getAbsolutePath(),true);
	          BufferedWriter bw = new BufferedWriter(fw);
	          
	         
	          String writeThis;
	          writeThis =  "ECG Recording" + "\r\n" ;
	          fw.append(writeThis);
	          
	          writeThis =  "File name: " + fname;
	          fw.append(writeThis);
	          fw.append("\r\n\r\n");
	          
	          writeThis =  "Patient name: " + userName;
	          fw.append(writeThis);
	          fw.append("\r\n");
	          
	          writeThis =  "Date: " + date;
	          fw.append(writeThis);
	          fw.append("\r\n");
	          
	          writeThis =  "Time: " + time;
	          fw.append(writeThis);
	          fw.append("\r\n\r\n");
	          
	          
	          writeThis =  "Sampling Rate: " + sampleRate + " Hz";
	          fw.append(writeThis);
	          
	          fw.append("\r\n");
	          fw.append("ECG Value");
	          fw.append("\r\n");
	          fw.close();
	          Log.d("Suceess","Sucess");
	          
	          return ;
	        } catch (IOException e) {
	          e.printStackTrace();
	          return ;
	        }
   }
   
   
   public void write(String fname, double fcontent, String fPath){
	  
      try {
        
        File file = new File(fPath, fname);
        // If file does not exists, then create it
        if (!file.exists()) {
          file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsolutePath(),true);
        /*BufferedWriter bw = new BufferedWriter(fw);
        
        String time;
		Calendar c = Calendar.getInstance();
		time = sdf.format(c.getTime());
        
        String writeThis;
        writeThis =fcontent;*/
    	fw.append(String.valueOf(fcontent));
    	fw.append("\r\n");
        
        fw.close();
        Log.d("Suceess","Sucess");
        return ;
      } catch (IOException e) {
        e.printStackTrace();
        return ;
      }
   }
   
   //
   
   public void write2(String fName, String fcontent, String fPath, int recordState){
	   File myFile = new File(fPath, fName);
	   
	  
   }
   //
   
   
   public void getTimeStamp(String time){
	   Time now = new Time();
		 now.setToNow();
		 time =  now.format("%H:%M:%S");
		 //time = getDate(System.currentTimeMillis(), "dd/MM/yyyy hh:mm:ss.SSS");
		 
	   
   }
   
   /**
    * Return date in specified format.
    * @param milliSeconds Date in milliseconds
    * @param dateFormat Date format 
    * @return String representing date in specified format
    */
   public static String getDate(long milliSeconds, String dateFormat)
   {
       // Create a DateFormatter object for displaying date in specified format.
       SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

       // Create a calendar object that will convert the date and time value in milliseconds to date. 
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
   }


}
