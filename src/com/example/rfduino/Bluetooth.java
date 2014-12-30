package com.example.rfduino;




import org.achartengine.GraphicalView;

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
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Bluetooth extends Activity implements SensorEventListener {
	private final static String TAG = "BLUETOOTH";
	
	private BluetoothAdapter myBluetoothAdapter;
	
	Button blueon, blueoff, bluecancel, bluesearch, sayHello, sayBye, stopBut, startBut;
	private Handler handler = new Handler();
	
	
	//Line variables
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private boolean start = false;
	private boolean firstTime = true;
	private int index, time;
	private static GraphicalView view;
	private ECGLine line = new ECGLine();
	private static Context context;
	
	public double[] data = { 0.000000, 0.099833, 0.198669, 0.295520, 0.389418, 0.479426, 0.564642, 0.644218, 0.717356, 0.783327, 0.841471, 0.891207, 0.932039, 0.963558, 0.985450, 0.997495, 0.999574, 0.991665, 0.973848, 0.946300, 0.909297, 0.863209, 0.808496, 0.745705, 0.675463, 0.598472, 0.515501, 0.427380, 0.334988, 0.239249, 0.141120, 0.041581, -0.058374, -0.157746, -0.255541, -0.350783, -0.442520, -0.529836, -0.611858, -0.687766, -0.756802, -0.818277, -0.871576, -0.916166, -0.951602, -0.977530, -0.993691, -0.999923, -0.996165, -0.982453, -0.958924, -0.925815, -0.883455, -0.832267, -0.772764, -0.705540, -0.631267, -0.550686, -0.464602, -0.373877, -0.279415, -0.182163, -0.083089, 0.016814, 0.116549, 0.215120, 0.311541, 0.404850, 0.494113, 0.578440, 0.656987, 0.728969, 0.793668, 0.850437, 0.898708, 0.938000, 0.967920, 0.988168, 0.998543, 0.998941, 0.989358, 0.969890, 0.940731, 0.902172, 0.854599, 0.798487, 0.734397, 0.662969, 0.584917, 0.501021, 0.412118, 0.319098, 0.222890, 0.124454, 0.024775, -0.075151, -0.174327, -0.271761, -0.366479, -0.457536, -0.544021, -0.625071, -0.699875, -0.767686, -0.827826, -0.879696, -0.922775, -0.956635, -0.980936, -0.995436, -0.999990, -0.994553, -0.979178, -0.954019, -0.919329, -0.875452, -0.822829, -0.761984, -0.693525, -0.618137, -0.536573, -0.449647, -0.358229, -0.263232, -0.165604, -0.066322};
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
		
		
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		blueon = (Button)findViewById(R.id.bluetoothOn);
		bluesearch =(Button)findViewById(R.id.bluetoothSearch);
		sayHello = (Button)findViewById(R.id.sayHello);
		sayBye = (Button)findViewById(R.id.sayBye);
		startBut = (Button)findViewById(R.id.start);
		stopBut = (Button)findViewById(R.id.stop);
		
		line.initialize();
		
		initButtons();
		
		IntentFilter intentFilter = new IntentFilter("ECG_EVENT");
        registerReceiver(broadcastRx, intentFilter);
		
	}
	
	 @Override
	 protected void onDestroy() {
	  super.onDestroy();
	  //un-register BroadcastReceiver
	  unregisterReceiver(broadcastRx);
	 }
	 
	 
	 @Override
	 protected void onResume() {
		super.onResume();
	    IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction("POSTURE_ACTION");
	    registerReceiver(broadcastRx, intentFilter);
	}
		
	@Override
	protected void onPause() {
		super.onPause();

		LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);   
	    bManager.unregisterReceiver(broadcastRx);
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
		blueon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 if(myBluetoothAdapter==null){
					 Toast.makeText(getApplicationContext(), "Bluetooth service not available in the device", Toast.LENGTH_SHORT).show();
	             }
				 else{
					 if(!myBluetoothAdapter.isEnabled()){
							Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(turnOn, 0);
							Toast.makeText(getApplicationContext(), "Bluetooth turned ON", Toast.LENGTH_SHORT).show();
						}else{
							Toast.makeText(getApplicationContext(), "Bluetooth is already ON", Toast.LENGTH_SHORT).show();
						}
					 }	
			}     
	    });
	
		bluesearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 if(myBluetoothAdapter!=null){
		        Intent intent = new Intent(Bluetooth.this, bleService.class);
		        startService(intent);
				 }
			}     
	    });
		
		sayHello.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 if(myBluetoothAdapter!=null){
					 bleService.send(new byte[] {0x00});
				 }
			}     
	    });
		
		sayBye.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 if(myBluetoothAdapter!=null){
					 bleService.send(new byte[] {(byte)0xFF});
				 }
			}     
	    });
		
		startBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startDAQ ();
			}     
	    });
		
		stopBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 stopDAQ();
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
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener( this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
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

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (start) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
				float x = event.values[0];
				
			// UPDATE GRAPH
				line.addPoint(time,data[index]);
				
				//line.addPoint(time,(double) x);
				//Get Graph information:
				
				GraphicalView lineView = line.getView(this);
				//Get reference to layout:
				LinearLayout layout =(LinearLayout)findViewById(R.id.chart);
				//clear the previous layout:
				layout.removeAllViews();
				//add new graph:
				layout.addView(lineView);
				}
				
				
				time++;
				index++;
				if (index == 126)
					index = 0;
				
				
				
			
			}
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	
	

	private BroadcastReceiver broadcastRx = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        
	        	float data0 = intent.getFloatExtra("ECGData0", 0.0f);
	        	float data1 = intent.getFloatExtra("ECGData1", 0.0f);
	        	float data2 = intent.getFloatExtra("ECGData2", 0.0f);
	        	float data3 = intent.getFloatExtra("ECGData3", 0.0f);
	        	float data4 = intent.getFloatExtra("ECGData4", 0.0f);
	        	
	        	Log.w(TAG,String.valueOf(data0)+ "," +String.valueOf(data1)
	        			 + "," +String.valueOf(data2)+ "," +String.valueOf(data3)+ 
	        			 "," +String.valueOf(data4));
	        	
	        	if (firstTime){
	        		time = 0;
	        		index =0;
	        		start = true;
	        		firstTime = false;
	        	}
	        	//for (int i = 0; i < data.length; i++){
	        	//line.addPoint(time,data[i]);
	        	//time++;
	        	//}
	        	line.addPoint(time,data0);
	        	time++;
	        	line.addPoint(time,data1);
	        	time++;
	        	line.addPoint(time,data2);
	        	time++;
	        	line.addPoint(time,data3);
	        	time++;
	        	line.addPoint(time,data4);
	        	time++;
				
				//line.addPoint(time,(double) x);
				//Get Graph information:
				GraphicalView lineView = line.getView(context);
				//Get reference to layout:
				LinearLayout layout =(LinearLayout)findViewById(R.id.chart);
				//clear the previous layout:
				layout.removeAllViews();
				//add new graph:
				layout.addView(lineView);
				
				
				
				time++;
	        	
	 
	        
	    }
	};
}
