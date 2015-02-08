package com.example.rfduino;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

public class dataSaveService extends Service {
	private final static String TAG = "DataSave";
	private final String PATH = Environment.getExternalStorageDirectory() + "/rfDuino/ECG Recordings";
	private final String userName = "Jane Doe";
	private final static short maxSampleSize = 18000; // 300 Hz * 60 seconds
	
	
	public String fileName;
	private Time now = new Time();
	private FileOperations fileOps = new FileOperations();
	
	public LinkedBlockingQueue<Float> queue = btMateService.bluetoothQueueForSaving;
	static writeDataThread writeThread;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override 
	public void onDestroy(){
		writeThread.cancel();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		// TODO link user name
		writeThread = new writeDataThread();
		writeThread.start();
		
		return START_STICKY;
	}
	
	class writeDataThread extends Thread{
		private boolean continueWriting = true;
		private double data;
		private short dataCount;
		
		public writeDataThread(){
			createNewFile();
			dataCount = 0;
			data = 0.0;
		}
		
		@Override
		public void run(){
			while(continueWriting){
				if (queue.size() >= 1){
					
					if (queue.size() >= 1){
						for (int i =0; i<=queue.size(); i++){
							try {
								data = (double) queue.poll(2,TimeUnit.SECONDS);
								fileOps.write(fileName, data, PATH);
								//Log.i(TAG,String.valueOf(data));
								
								dataCount++;
								if (dataCount > maxSampleSize){
									createNewFile();
					 				dataCount =0;
								}
								
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
					}
			}
		}
		
		public void cancel(){
			continueWriting = false;
		}
		
		public void createNewFile(){
			now.setToNow();
			fileName = userName+ ' ' + now.format("%m-%d-%Y %H:%M:%S") + ".csv";
			
			fileOps.writeHeader(fileName, PATH, userName, now.format("%m-%d-%Y"), now.format("%H:%M:%S"));
			Log.i(TAG,"Created file");
		}
		
		
	}

}
