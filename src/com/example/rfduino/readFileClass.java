package com.example.rfduino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Time;
import android.util.Log;

public class readFileClass {
	private Activity mParent = null; 
	private Context context;
	public String name, todaysDate;
	public String[] split;
	private final String TAG = "DEBUG";
	private Time now = new Time();
	
	public postureLine dataLine = new postureLine();
	public Boolean newDay;
	
	private String path; 
	
	static LinkedBlockingQueue<Float>  readQueueForUI = new LinkedBlockingQueue<Float>();
	
	public readFileClass(Activity parent){
		this.mParent = parent;
		mParent.runOnUiThread(new Runnable(){
	        public void run() {
	        	dataLine.initialize();
	        }
	    });
		now.setToNow();
		todaysDate = now.format("%m-%d-%Y"); // works
		//todaysDate ="01-18-2015";
		
		path = Environment.getExternalStorageDirectory() + "/rfDuino/ECG Recordings/" + "Jane Doe 01-18-2015 15:30:08.csv";
		newDay = true;
	}
	
	public String readCurrentFile(){
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard, "/rfDuino/ECG Recordings");
		for (File f : dir.listFiles()) {
		    if (f.isFile()){
		        name = f.getName();
		    	split = name.split("\\s+");
		        Log.i(TAG,split[2]);
		        Log.e(TAG,path);
		        if (split[2].equals(todaysDate)){
		        	// not a new day
		        	newDay = false;
		        	path = f.getAbsolutePath();
		        	
		        	
		        	
		        }
		        else{
		        	newDay = true;
		        	
		        }
		    }
		}
		return path;
	}
	
	public void readFile(String filePath){
		BufferedReader br = null;
		String line = "";
		
		int m = 0;
		try {
			 
			br = new BufferedReader(new FileReader(filePath));
			
			//Skip headers
			for (int i =0; i <9; i++)
				line = br.readLine(); 
			
			//Output the important plot data
			while ((line = br.readLine()) != null) {
				String[] data = line.split(",");
				Log.i(TAG,data[0]);
				
				
				dataLine.addPoint(m, Double.parseDouble(data[0]));
				
				
				
			
				m++;
				
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		
	
		
	
		
	
	}
	
	
}
