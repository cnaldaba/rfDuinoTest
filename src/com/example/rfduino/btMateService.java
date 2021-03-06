package com.example.rfduino;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class btMateService extends Service {
	 private final String TAG = "btMateService";
	 private BluetoothManager mBluetoothManager;
	 private BluetoothAdapter mBluetoothAdapter;
	 private BluetoothSocket mSocket;
	 private BluetoothDevice mDevice;
	 private final String BTMate_MAC = "DD:FF:4E:66:1A:D4";
	 private final String BTMate_name = "G01ECG";
	 private final UUID BTMate_UUID = UUID.fromString("d80bed9f-9894-4e55-b2ca-c1d1778368a1");
	 private final  UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); 
	
	 private final char START = 0x00;
	 private final char STOP = 0xFF;
	 private final char DISCONNECT = 0x55;
	 
	 private ConnectThread mateConnect;
	 private ReadThread mateRead;
	 
	 public long pastMsTime, nowMsTime, duration;
	 
	 public  enum connectState {CONNECTED, DISCONNECTED};
	 public  enum deviceState {IDLE, READ};
	 
	 public connectState mateConnected;
	 
	 static LinkedBlockingQueue<Float>  bluetoothMateQueueForUI = new LinkedBlockingQueue<Float>();
	 static LinkedBlockingQueue<Float>  bluetoothQueueForSaving = new LinkedBlockingQueue<Float>();
	 
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy(){
		unregisterReceiver(mReceiver);
		unregisterReceiver(mACTReceiver);
		try {
			mSocket.close();
			Log.d(TAG,"mSocket closing");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 Log.d(TAG,"mateRead Thread closing");
		 mateRead.writeByte((char) 0xFF);
		 mateRead.cancel();
	}
	
	@Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		mateConnected = connectState.DISCONNECTED;
		
		mSocket = null;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		IntentFilter filter = new IntentFilter();
		 
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		 
		registerReceiver(mReceiver, filter);
		mBluetoothAdapter.startDiscovery();
		
		IntentFilter intentFilter = new IntentFilter("BTMATE_EVENT");
        registerReceiver(mACTReceiver, intentFilter);
		
		return super.onStartCommand(intent, flags, startId);
		
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			 
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                       //bluetooth device found
                final BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, device.getName());
                if (device.getName().equals(BTMate_name)){
                mBluetoothAdapter.cancelDiscovery();
                mDevice = device;
                mateConnect = new ConnectThread();
                mateConnect.start();
                
    			
                }
            }
     
		};
	};
	
	
	
	/*
	 *  This handles the bluetooth connection
	 *  
	 * */
	private class ConnectThread extends Thread {
	 
	 
	    public ConnectThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	    	
	    	if (mBluetoothAdapter.isDiscovering())
	    		mBluetoothAdapter.cancelDiscovery();
	    	
	        BluetoothSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
	        } catch (IOException e) { }
	        mSocket = tmp;
	        
	    }
	    
	    public void run(){
	    	try {
	    		Log.d(TAG, "+++ Connecting...");
				mSocket.connect();
				mateConnected = connectState.CONNECTED;
				mateRead = new ReadThread(mSocket);
				mateRead.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	 

	}
	
	
	/* 
	 * This thread handles reading / writing to the BTMate
	 * 
	 * */
	
	private class ReadThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    private Boolean status, startOfInt;
	    private short value;
	    private char ptr, guiPTR;
	    public deviceState mateState;
	    public boolean saveData;
	    private   ByteBuffer bb;
	    
	    public ReadThread(BluetoothSocket socket) {
	    	saveData = false;
	    	mateState = deviceState.IDLE;
	        value = 0;
	    	startOfInt = false;
	    	ptr = 0;
	    	guiPTR = 0;
	    	status = true;
	    	mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	        bb = ByteBuffer.allocate(2);
	        
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	        
	        startStream();
	       
	       
	    }
	 
	    public void run() {
	        //char c;
	      
	        bb.order(ByteOrder.LITTLE_ENDIAN);
	        byte b;
	        int numBytes;
	        // Keep listening to the InputStream until an exception occurs
	        while (status) {
	            try {
	                // Read from the InputStream
	            	//numBytes = mmInStream.read(buffer);
	            	
	            	numBytes = mmInStream.available();
	            	
	            	for (int i =0; i<= numBytes; i++){
	                b = (byte) mmInStream.read();
	            
	                if(startOfInt){
	                	bb.put(b);
	                	
	                	
	                	if (ptr == 1){
	                		value = bb.getShort(0);
	                		//Log.d(TAG, String.valueOf(value));
	                		bb.clear();
	                		ptr = 0;
	                		startOfInt = false;
	                		
	                		//FOR GUI PLOT
	                		if (guiPTR == 2){
	                			guiPTR = 0;
	                			bluetoothMateQueueForUI.offer((float) value);
	                		}
	                		else{
	                			guiPTR++;
	                		}
	                		
	                		//FOR SAVING SERVICE
	                		if (saveData){
	                			 bluetoothQueueForSaving.offer((float) value);
	                		}
	                	}
	                	else{
	                		ptr++;
	                	}
	            	}
	                
	                if (b == '\n'){
	                	startOfInt = true;
	                }
	            	}
	            	
	                // Send the obtained bytes to the UI activity
	                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    public void startStream(){
	    	   try {
					mmOutStream.write(START);
					mateState = deviceState.READ;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	    
	    public void stopStream(){
	    	try {
				mmOutStream.write(DISCONNECT);
				mateState = deviceState.IDLE;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    /* Call this from the main activity to send data to the remote device */
	    public void writeBytes(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        }
	        catch (IOException e) { }
	    }
	 
	    public void writeByte(char b) {
	        try {
	            mmOutStream.write(b);
	        } catch (IOException e) { }
	    }
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	    	  try {
					mmOutStream.write(DISCONNECT);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	  try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	        
	        if (saveData){
	        	Intent i = new Intent(btMateService.this, dataSaveService.class);
	        	stopService(i);
	        }
	    }
	}
	
	
	// RECIEVER TO GET SHIT FROM ACTIVITY
	private final BroadcastReceiver mACTReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			char action = intent.getCharExtra("command", '0');
			Intent i = new Intent(btMateService.this, dataSaveService.class);
			if (action == 's'){
				if ( (mateConnected == connectState.CONNECTED) & (mateRead.mateState == deviceState.IDLE)){
				
				mateRead.startStream();
				}
			}
			else if (action == 'p'){
				if ( (mateConnected == connectState.CONNECTED) & (mateRead.mateState == deviceState.READ)){
			    // stop receiving data device
				mateRead.stopStream();
				}
			}
			else if(action == 'r'){
				if ( (mateConnected == connectState.CONNECTED) & (mateRead.mateState == deviceState.READ)){
					mateRead.saveData = true;
					startService(i);
				}
			}
			else if(action == 'n'){
				if ( (mateConnected == connectState.CONNECTED) & (mateRead.mateState == deviceState.READ)){
					mateRead.saveData = false;
					stopService(i);
					bluetoothQueueForSaving.clear();
				}
			}
			 
        
     
		};
	};
	
	
}
