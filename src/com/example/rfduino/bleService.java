package com.example.rfduino;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


public class bleService  extends Service{
	 private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CANADA);
	 private final static String TAG = bleService.class.getSimpleName();
	 private final static String DEBUG = "DEBUG";
	
	 private Handler handler = new Handler();
	 Context context;
	 
	 private BluetoothManager mBluetoothManager;
	 private BluetoothAdapter mBluetoothAdapter;
	 private String mBluetoothDeviceAddress;
	 private static BluetoothGatt mBluetoothGatt;
	 private BluetoothGatt mConnectedGatt;
	 private static BluetoothGattService mBluetoothGattService;
	 
	 private static final long SCAN_PERIOD = 2500;  // Used to scan for devices for only 10 secs
	
	 private enum mDeviceState {CONNECTED, DISCONNECTED};
	 private mDeviceState rfDuinoState;
	 //-----------------------------------------------
	 // RFDUINO DATA
	 //-----------------------------------------------
	 public final static String ACTION_CONNECTED =
	            "com.rfduino.ACTION_CONNECTED";
	 public final static String ACTION_DISCONNECTED =
	            "com.rfduino.ACTION_DISCONNECTED";
	 public final static String ACTION_DATA_AVAILABLE =
	            "com.rfduino.ACTION_DATA_AVAILABLE";
	 public final static String EXTRA_DATA =
	            "com.rfduino.EXTRA_DATA";
	
	 public final static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
	 public final static UUID UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
	 public final static UUID UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
	 public final static UUID UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
	 public final static UUID UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);
	 
	 private final String rfDuino_MAC = "DD:FF:4E:66:1A:D4";
	 
	 
	 private static float floatValue0,floatValue1,floatValue2,floatValue3,floatValue4;
	 private static float lastValue0;
	 
	 private enum serviceState {RECORD, IDLE};
	 private serviceState actionState;
	 
	 
	 static LinkedBlockingQueue<Float>  bluetoothQueueForUI = new LinkedBlockingQueue<Float>();
	 static LinkedBlockingQueue<Float>  bluetoothQueueForSaving = new LinkedBlockingQueue<Float>();
	 //***********************************************
	 // SERVICE FUNCTIONS
	 //***********************************************
	 @Override
		public void onCreate(){
			
		}
	 
	@Override
		public void onDestroy(){ // disconnects the sensortag connection after quitting service
		
        Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable(){
			@Override
			public void run(){
				send(new byte[] {(byte)0x00});
			}
		});
		
		unregisterReceiver(receiver);
		
		
		h.postDelayed(new Runnable(){
			@Override
			public void run(){
				mBluetoothGatt.disconnect();	
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				Log.i(DEBUG, "Disconnected");
			}
		}, 250);
		
		
		}
	 
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
        bleService getService() {
            return bleService.this;
        }
    }
	
	@Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
    
    
	@Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		initialize();
		rfDuinoState = mDeviceState.DISCONNECTED;
		actionState = serviceState.IDLE;
		
        IntentFilter intentFilter = new IntentFilter("ECG_EVENT");
        registerReceiver(receiver, intentFilter);
		
		lastValue0 = 0.0f;
		MyThread myThread = new MyThread(); // creating a new thread?
		myThread.start();
		return  super.onStartCommand(intent, flags, startId);
	}
	

	
	public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
	 //***********************************************
	 // THREAD
	 //***********************************************
	final class MyThread extends Thread{
		 
		 @Override
		 public void run() {
		  // TODO Auto-generated method stub
		startScan();
  
	
	
		 }	
	}
	
	 //***********************************************
	 // BLE FUNCTIONS
	 //***********************************************
	public void startScan(){
		 
		 mBluetoothAdapter.startLeScan(mLeScanCallback);

		 Log.i(DEBUG, "start scan");
		 Handler h = new Handler(Looper.getMainLooper()); //handler to delay the scan, if can't connect, then stop attempts to scan
		 h.postDelayed(mStopScanRunnable, SCAN_PERIOD);
	 }
	
	public void stopScan(){
		Log.i(DEBUG, "Stop scan");
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}

	private Runnable mStopScanRunnable = new Runnable() {
	    @Override
	    public void run() {
	        stopScan();
	    }
	};
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = 
			new BluetoothAdapter.LeScanCallback(){
			@Override
			public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
				Handler h = new Handler(Looper.getMainLooper());
				h.post(new Runnable(){
					@Override
					public void run(){
						Log.i("BLE", "New LE Device: " + device.getName() + " @ " + rssi);
						if (device.getAddress().equals(rfDuino_MAC) && (rfDuinoState == mDeviceState.DISCONNECTED)){
							mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
							 stopScan();
						}
					}
				});
				}
			};
			
			
	//***********************************************
	// GATTCALLBACK FUNCTIONS
	//***********************************************
			
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
				
				/* What occurs once the device is connected:*/
		
		
				@Override
			    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
					
					   if (newState == BluetoothProfile.STATE_CONNECTED) {
			                Log.i(TAG, "Connected to RFduino.");
			                Log.i(TAG, "Attempting to start service discovery:" +
			                		mBluetoothGatt.discoverServices());
			                rfDuinoState = mDeviceState.CONNECTED;
			            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			                Log.i(TAG, "Disconnected from RFduino.");
			                //broadcastUpdate(ACTION_DISCONNECTED);
			                rfDuinoState = mDeviceState.DISCONNECTED;
			            }
			       
			    }
				
				@Override
				/* New services connected*/
			    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
					if (status == mConnectedGatt.GATT_SUCCESS) {
		                mBluetoothGattService = gatt.getService(UUID_SERVICE);
		                if (mBluetoothGattService == null) {
		                    Log.e(TAG, "RFduino GATT service not found!");
		                    return;
		                }

		                BluetoothGattCharacteristic receiveCharacteristic =
		                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
		                if (receiveCharacteristic != null) {
		                    BluetoothGattDescriptor receiveConfigDescriptor =
		                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
		                    if (receiveConfigDescriptor != null) {
		                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

		                        receiveConfigDescriptor.setValue(
		                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		                        gatt.writeDescriptor(receiveConfigDescriptor);
		                    } else {
		                        Log.e(TAG, "RFduino receive config descriptor not found!");
		                    }

		                } else {
		                    Log.e(TAG, "RFduino receive characteristic not found!");
		                }

		                //broadcastUpdate(ACTION_CONNECTED);
		                Handler h = new Handler(Looper.getMainLooper());
						h.postDelayed(new Runnable(){
							@Override
							public void run(){
								send(new byte[] {(byte)0x55});
							}
						}, 250);
		                
						h.postDelayed(new Runnable(){
								@Override
								public void run(){
									send(new byte[] {(byte)0xFF});
								}
							}, 250);
						
						handler.postDelayed(runnablePlot, 100);
						
		            } else {
		                Log.w(TAG, "onServicesDiscovered received: " + status);
		            }
			    }
				
			       @Override
			        public void onCharacteristicRead(BluetoothGatt gatt,
			                                         BluetoothGattCharacteristic characteristic,
			                                         int status) {
			            if (status == mConnectedGatt.GATT_SUCCESS) {
			            	  Log.w(TAG, "Characteristic Read");
			               // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			            }
			        }

			        @Override
			        public void onCharacteristicChanged(BluetoothGatt gatt,
			                                            BluetoothGattCharacteristic c) {
			        	  Log.w(TAG, "Characteristic Changed");
			        	  int asInt;
			        	  byte[] rawValue = c.getValue();
			        	  
			        	  asInt = (rawValue[0] & 0xFF) | ((rawValue[1] & 0xFF) << 8) 
			        			  | ((rawValue[2] & 0xFF) << 16) | ((rawValue[3] & 0xFF) << 24);
			        	  floatValue0 =  Float.intBitsToFloat(asInt);
			        	  
			        	  asInt = (rawValue[4] & 0xFF) | ((rawValue[5] & 0xFF) << 8) 
			        			  | ((rawValue[6] & 0xFF) << 16) | ((rawValue[7] & 0xFF) << 24);
			        	  floatValue1 =  Float.intBitsToFloat(asInt);
			        	  
			        	  asInt = (rawValue[8] & 0xFF) | ((rawValue[9] & 0xFF) << 8) 
			        			  | ((rawValue[10] & 0xFF) << 16) | ((rawValue[11] & 0xFF) << 24);
			        	  floatValue2 =  Float.intBitsToFloat(asInt);
			        	  
			        	  asInt = (rawValue[12] & 0xFF) | ((rawValue[13] & 0xFF) << 8) 
			        			  | ((rawValue[14] & 0xFF) << 16) | ((rawValue[15] & 0xFF) << 24);
			        	  floatValue3 =  Float.intBitsToFloat(asInt);
			        	  
			        	  asInt = (rawValue[16] & 0xFF) | ((rawValue[17] & 0xFF) << 8) 
			        			  | ((rawValue[18] & 0xFF) << 16) | ((rawValue[19] & 0xFF) << 24);
			        	  floatValue4 =  Float.intBitsToFloat(asInt);
			        	  
			        	  Handler h = new Handler(Looper.getMainLooper());
							h.post(new Runnable(){
								@Override
								public void run(){
									send(new byte[] {(byte)0x55});
								}
							});
							
			        	 //Place data onto FIFO 
						 bluetoothQueueForUI.offer(floatValue0);
						 bluetoothQueueForUI.offer(floatValue1);
						 bluetoothQueueForUI.offer(floatValue2);
						 bluetoothQueueForUI.offer(floatValue3);
						 bluetoothQueueForUI.offer(floatValue4);
						 
						 if (actionState == serviceState.RECORD){
							 bluetoothQueueForSaving.offer(floatValue0);
							 bluetoothQueueForSaving.offer(floatValue1);
							 bluetoothQueueForSaving.offer(floatValue2);
							 bluetoothQueueForSaving.offer(floatValue3);
							 bluetoothQueueForSaving.offer(floatValue4);
						 }
						 
			        	 /* String strValue;
						try {
							strValue = new String(rawValue, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        	 // int intValue = 0;
			        	  
			        	  strValue = HexAsciiHelper.bytesToAsciiMaybe(rawValue);
			        	  if (!strValue.equals(null))
			        	  Log.e(TAG, strValue);*/
			           // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			              String time;
			              //Time now = new Time();
			      		 //now.setToNow();
			      		 //long millis = System.currentTimeMillis();
			      		 //time =  now.format("%H:%M:%S");
			      		 Calendar cal = Calendar.getInstance();
			      		 
			      		 time = sdf.format(cal.getTime());
			        	 Log.e(TAG, time + "- " +String.valueOf(floatValue0)+ "," +String.valueOf(floatValue1)
			        			 + "," +String.valueOf(floatValue2)+ "," +String.valueOf(floatValue3)+ 
			        			 "," +String.valueOf(floatValue4));
			      		//Log.e(TAG, String.valueOf(rawValue.length));
			        	
			          lastValue0 = floatValue0;

			        	
			        }
			}; //End of mGattCallback
			
	   //***********************************************
	   // RFDUINO FUNCTIONS
	   //***********************************************	
			 public void read() {
			        if (mBluetoothGatt == null || mBluetoothGattService == null) {
			            Log.w(TAG, "BluetoothGatt not initialized");
			            return;
			        }

			        BluetoothGattCharacteristic characteristic =
			                mBluetoothGattService.getCharacteristic(UUID_RECEIVE);

			        mBluetoothGatt.readCharacteristic(characteristic);
			    }

			    public static boolean send(byte[] data) {
			        if (mBluetoothGatt == null || mBluetoothGattService == null) {
			            Log.w(TAG, "BluetoothGatt not initialized");
			            return false;
			        }

			        BluetoothGattCharacteristic characteristic =
			                mBluetoothGattService.getCharacteristic(UUID_SEND);

			        if (characteristic == null) {
			            Log.w(TAG, "Send characteristic not found");
			            return false;
			        }

			        characteristic.setValue(data);
			        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			        Log.w(TAG, "Write");
			        return mBluetoothGatt.writeCharacteristic(characteristic);
			    }
	  //***********************************************
	  // MISC FUNCTIONS
	  //***********************************************
			
	  public boolean initialize() {
		     if (mBluetoothManager == null) {
		            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		            if (mBluetoothManager == null) {
		                Log.e(TAG, "Unable to initialize BluetoothManager.");
		                return false;
		            }
		        }

		        mBluetoothAdapter = mBluetoothManager.getAdapter();
		        if (mBluetoothAdapter == null) {
		            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
		            return false;
		        }

		        return true;
	    }
	  
	  
	  private Runnable runnablePlot = new Runnable() {
		   @Override
		   public void run() {
			  
		
			  if(lastValue0 != 0.0f){
				Intent i = new Intent("ECG_EVENT");
	     			
   				i.putExtra("ECGData0", Float.parseFloat(String.valueOf(floatValue0)));
   				i.putExtra("ECGData1", floatValue1);
   				i.putExtra("ECGData2", floatValue2);
   				i.putExtra("ECGData3", floatValue3);
   				i.putExtra("ECGData4", floatValue4);
   				
   				sendBroadcast(i);
			  
			  }
			  handler.postDelayed(this, 100);
			  
		   }};
		   
		   
		   private final BroadcastReceiver receiver = new BroadcastReceiver() {
			   @Override
			   public void onReceive(Context context, Intent intent) {
			      int command = intent.getIntExtra("command", 0);
			      
			      Intent i = new Intent(bleService.this, dataSaveService.class);
			      
			      
			      switch(command){
			      case 1:
			    	  // Start recording
			    	  if(rfDuinoState == mDeviceState.CONNECTED){
					  startService(i);
					  actionState = serviceState.RECORD;
			    	  }
			    	  else{
			    		Handler h = new Handler(Looper.getMainLooper());
			      		h.post(new Runnable(){
			      			@Override
			      			public void run(){	
			      				Toast.makeText( bleService.this, "rfDuino not connected", Toast.LENGTH_SHORT).show();
			      			}
			      		});
			    	  }
			    	  break;
			      case 2:
			    	  // stop recording
					  stopService(i);
					  actionState = serviceState.IDLE;
			    	  break;
			      default:
			    	  // do nothing
			    	  break;
			    	  
			      }
			    		  
			      
			 
			   }
			};

}
