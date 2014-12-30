package com.example.rfduino;




import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Bluetooth extends Activity {
	private BluetoothAdapter myBluetoothAdapter;
	
	Button blueon, blueoff, bluecancel, bluesearch, sayHello, sayBye;
	private Handler handler = new Handler();
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);
		
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		blueon = (Button)findViewById(R.id.bluetoothOn);
		bluesearch =(Button)findViewById(R.id.bluetoothSearch);
		sayHello = (Button)findViewById(R.id.sayHello);
		sayBye = (Button)findViewById(R.id.sayBye);
		
		
		initButtons();
		
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
		
		
	}
}
