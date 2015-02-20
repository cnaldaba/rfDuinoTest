package com.example.rfduino;




import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesRenderer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Bluetooth extends Activity {
	private  Chronometer timer;
	
	private final static String TAG = "BLUETOOTH";
	private String readFilePath;
	
	private BluetoothAdapter myBluetoothAdapter;
	private rfDuinoClass rfDuino;
	private readFileClass readFile;
	private static Context context;
	
	Button blueon, blueoff, bluecancel, bluesearch, sayHello, sayBye, stopBut, startBut,viewBut;
	Button BTDiscover, BTDisconnect;
	ToggleButton recordBut, playPauseBut;
	
	private Handler handler = new Handler();
	
	
	//Line variables
	private XYSeries xySeries;
	private XYMultipleSeriesDataset dataset;
	private XYMultipleSeriesRenderer renderer;
	private XYSeriesRenderer rendererSeries;
	private GraphicalView view;
	
	private boolean start = false;
	private boolean firstTime = true;
	private int index, time;
	
	private int pointsToDisplay = 75;
	private int yMax = 15;
	private int yMin = 0;
	private int xScrollAhead = 35;
	
	private ECGLine line = new ECGLine();
	private final int chartDelay = 3; // millisecond delay for count
	public LinkedBlockingQueue<Float> queue = btMateService.bluetoothMateQueueForUI;
	public LinkedBlockingQueue<Float> readQueue = readFileClass.readQueueForUI;
	
	private float samplingRate = 0.003f; 
	private float currentX;
	private ChartThread chartThread;
	
	
	public double[] data = { 0.000000, 0.099833, 0.198669, 0.295520, 0.389418, 0.479426, 0.564642, 0.644218, 0.717356, 0.783327, 0.841471, 0.891207, 0.932039, 0.963558, 0.985450, 0.997495, 0.999574, 0.991665, 0.973848, 0.946300, 0.909297, 0.863209, 0.808496, 0.745705, 0.675463, 0.598472, 0.515501, 0.427380, 0.334988, 0.239249, 0.141120, 0.041581, -0.058374, -0.157746, -0.255541, -0.350783, -0.442520, -0.529836, -0.611858, -0.687766, -0.756802, -0.818277, -0.871576, -0.916166, -0.951602, -0.977530, -0.993691, -0.999923, -0.996165, -0.982453, -0.958924, -0.925815, -0.883455, -0.832267, -0.772764, -0.705540, -0.631267, -0.550686, -0.464602, -0.373877, -0.279415, -0.182163, -0.083089, 0.016814, 0.116549, 0.215120, 0.311541, 0.404850, 0.494113, 0.578440, 0.656987, 0.728969, 0.793668, 0.850437, 0.898708, 0.938000, 0.967920, 0.988168, 0.998543, 0.998941, 0.989358, 0.969890, 0.940731, 0.902172, 0.854599, 0.798487, 0.734397, 0.662969, 0.584917, 0.501021, 0.412118, 0.319098, 0.222890, 0.124454, 0.024775, -0.075151, -0.174327, -0.271761, -0.366479, -0.457536, -0.544021, -0.625071, -0.699875, -0.767686, -0.827826, -0.879696, -0.922775, -0.956635, -0.980936, -0.995436, -0.999990, -0.994553, -0.979178, -0.954019, -0.919329, -0.875452, -0.822829, -0.761984, -0.693525, -0.618137, -0.536573, -0.449647, -0.358229, -0.263232, -0.165604, -0.066322};
	
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CANADA);
	public String path = Environment.getExternalStorageDirectory() + "/rfDuino/ECG Recordings" + "Jane Doe 01-18-2015 15:30:08.csv";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
		
		
		context = getBaseContext();
		rfDuino =  new rfDuinoClass(this);
		
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//blueon = (Button)findViewById(R.id.bluetoothOn);
		//bluesearch =(Button)findViewById(R.id.bluetoothSearch);
		//sayHello = (Button)findViewById(R.id.sayHello);
		//sayBye = (Button)findViewById(R.id.sayBye);
		startBut = (Button)findViewById(R.id.start);
		stopBut = (Button)findViewById(R.id.stop);
		viewBut = (Button)findViewById(R.id.view);
		BTDiscover = (Button)findViewById(R.id.discoverBTMate);
		BTDisconnect = (Button)findViewById(R.id.disconnectBTMate);
		recordBut = (ToggleButton)findViewById(R.id.record);
		//playPauseBut = (ToggleButton)findViewById(R.id.playPause);
		
		
		line.initialize();
		currentX = 0.0f;
		initButtons();
		
		//IntentFilter intentFilter = new IntentFilter("ECG_EVENT");
       // registerReceiver(broadcastRx, intentFilter);
        
		 ChartHandler chartUIHandler = new ChartHandler();
		 chartThread = new ChartThread(chartUIHandler);
		 chartThread.start();
		 
		 createAppFolder();
		 createECGFolder();
		 readFile = new readFileClass(this);
		 
		 paintGraph();
		
	}
	
	 @Override
	 protected void onDestroy() {
	  super.onDestroy();
	  //un-register BroadcastReceiver
	 // unregisterReceiver(broadcastRx);
	  
	  Intent i = new Intent("BTMATE_EVENT");
		i.putExtra("command", 'p');
		sendBroadcast(i);
		
		
	  Intent intent = new Intent(Bluetooth.this, bleService.class);
	  stopService(intent);
	  
	  Intent intent2 = new Intent(Bluetooth.this, btMateService.class);
	  stopService(intent2);
	  
	 }
	 
	 
	 @Override
	 protected void onResume() {
		super.onResume();
	}
		
	@Override
	protected void onPause() {
		Intent i = new Intent("BTMATE_EVENT");
		i.putExtra("command", 'p');
		sendBroadcast(i);
		
		 Intent intent2 = new Intent(Bluetooth.this, btMateService.class);
		  stopService(intent2);
		super.onPause();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	public void initButtons(){
		
		
		startBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//startDAQ ();
				 Intent i = new Intent("BTMATE_EVENT");
				 i.putExtra("command", 's');
			     sendBroadcast(i);
			}     
	    });
		
		stopBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// stopDAQ();
			     Intent i = new Intent("BTMATE_EVENT");
				 i.putExtra("command", 'p'); // stop recieving data
			     sendBroadcast(i);
			}     
	    });
		
		
		recordBut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(recordBut.isChecked()){  // Start recording
					  Intent i = new Intent("BTMATE_EVENT");
						i.putExtra("command", 'r');
						sendBroadcast(i);
				}
				else{ // stop recording

					 Intent i = new Intent("BTMATE_EVENT");
					 i.putExtra("command", 'n');
						sendBroadcast(i);
				}
				
			}
		});
		
		viewBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				readFilePath = readFile.readCurrentFile();
				
				if (readFile.newDay){
					// TODO create new file, don't plot
				}
				else{
					// TODO read file, plot data
					readFile.readFile(readFilePath);
					
					GraphicalView lineView = readFile.dataLine.getView(context);
					//Get reference to layout:
					LinearLayout layout =(LinearLayout)findViewById(R.id.chart);
					//clear the previous layout:
					layout.removeAllViews();
					//add new graph:
					layout.addView(lineView);
				}
			}     
	    });
		
		BTDiscover.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				Intent intent = new Intent(Bluetooth.this, btMateService.class);
				startService(intent);
			}
		});
		BTDisconnect.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				Intent intent = new Intent(Bluetooth.this, btMateService.class);
				stopService(intent);
			}
		});
		

		
	}
	
	
	
	public void stopDAQ (){
		start = false;
		line.stop();
		time =0;
		firstTime=true;
			
	}
	
	public void startDAQ (){
		if (firstTime){
		//line.initialize();
		// initialize accelerometers
	
		
		firstTime=false;
		time = 0;
		

		}
		index =0;
		start = true;
		
	}

	public void pauseDAQ(){
		start = false;
		line.stop();
		time =0;
		firstTime=true;
		
	}


	class ChartHandler extends Handler{
		@Override
		public void handleMessage(Message msg) {
			
			double yVal = ((double)msg.arg1)/1000;
			
			//Log.e(TAG,String.valueOf(yVal));
			
        	if (firstTime){
        		time = 0;
        		index =0;
        		start = true;
        		firstTime = false;
        	}
			
        	line.addPoint(time, yVal);
        	time++;
        	
        	
        	//line.rePaint();
			//Get Graph information:
			GraphicalView lineView = line.getView(context);
			//Get reference to layout:
			LinearLayout layout =(LinearLayout)findViewById(R.id.chart);
			//clear the previous layout:
			layout.removeAllViews();
			//add new graph:
			layout.addView(lineView);
			}
	}
	
	class ChartThread extends Thread{
		public boolean continuePlot = true;
		private Handler handler;
		
		
		public ChartThread(Handler handler){
			GraphicalView lineView = line.getView(context);
			this.handler = handler;
		}
		
		@Override
		public void run(){
			
			while(continuePlot){
				
				double yVal = 0;
				
				try {
					Thread.sleep(chartDelay);
					if (queue.size() >= 1){
					yVal = (double) queue.poll();
					currentX = currentX + samplingRate;
					Message msg = Message.obtain();
					msg.arg1 = (int)Math.round(yVal*1000);
					handler.sendMessage(msg);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				
				/*if (yVal != 0.0f){		
				currentX = currentX + samplingRate;
				
				Message msg = Message.obtain();
				msg.arg1 = (int)Math.round(yVal*1000);
				handler.sendMessage(msg);	
				}*/
			}	
		}
		
		public void cancel(){
			continuePlot = false;
		}
	}
	
	
  	private void createAppFolder(){
  		final String PATH = Environment.getExternalStorageDirectory() + "/rfDuino/";
  		if(!(new File(PATH)).exists()) 
  		new File(PATH).mkdirs();
  	}
  	
  	private void createECGFolder(){
  		final String PATH = Environment.getExternalStorageDirectory() + "/rfDuino/ECG Recordings";
  		if(!(new File(PATH)).exists()) 
  		new File(PATH).mkdirs();
  	}
	
	
	public void paintGraph(){
		//Get Graph information:
		GraphicalView lineView = line.getView(context);
		//Get reference to layout:
		LinearLayout layout =(LinearLayout)findViewById(R.id.chart);
		//clear the previous layout:
		layout.removeAllViews();
		//add new graph:
		layout.addView(lineView);
	}
	
	
}
