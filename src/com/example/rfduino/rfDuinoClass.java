package com.example.rfduino;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import org.achartengine.GraphicalView;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;


public class rfDuinoClass {
	 private ECGLine line = new ECGLine();
	 private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CANADA);
	 private final static String TAG = rfDuinoClass.class.getSimpleName();
	 private final static String DEBUG = "DEBUG";
	 
	 private final Activity parent;
	 private Handler handler = new Handler();
	 protected Bluetooth _context;
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
	 
	 
	 public static float floatValue0,floatValue1,floatValue2,floatValue3,floatValue4;
	 public int index;
	 public boolean firstTime;
	 
	 public rfDuinoClass(Activity parent){
		 rfDuinoState = mDeviceState.DISCONNECTED;
		 index = 0;
		 this.parent=parent;
		 context = parent.getBaseContext();
		 initialize(parent.getBaseContext());
		 
		 
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
			            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			                Log.i(TAG, "Disconnected from RFduino.");
			                //broadcastUpdate(ACTION_DISCONNECTED);
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
			        	  

			             String time;

			      		 Calendar cal = Calendar.getInstance();
			      		 
			      		 time = sdf.format(cal.getTime());
			        	 Log.e(TAG, time + "- " +String.valueOf(floatValue0)+ "," +String.valueOf(floatValue1)
			        			 + "," +String.valueOf(floatValue2)+ "," +String.valueOf(floatValue3)+ 
			        			 "," +String.valueOf(floatValue4));
			      		
			        	 if (firstTime){
				        		index =0;
				        		firstTime = false;
				        	}
			        	 
			        	 
			        
				        	
							parent.runOnUiThread(new Runnable(){
								
								public void run(){
									Log.e(DEBUG, "In Ui Thread");
									 line.addPoint(index,floatValue0);
										index++;
				    		GraphicalView lineView = line.getView(parent);
							//Get reference to layout:
							LinearLayout layout =(LinearLayout)parent.findViewById(R.id.chart);
							//clear the previous layout:
							layout.removeAllViews();
							//add new graph:
							layout.addView(lineView);
								}
							});
							
			        	 /*h.post(new Runnable(){
								@Override
								public void run(){
									line.addPoint(index,floatValue0);
									index++;
						        	line.addPoint(index,floatValue1);
						        	index++;
						        	line.addPoint(index,floatValue2);
						        	index++;
						        	line.addPoint(index,floatValue3);
						        	index++;
						        	line.addPoint(index,floatValue4);
						        	index++;
						        	
						    		GraphicalView lineView = line.getView(context);
									//Get reference to layout:
									LinearLayout layout =(LinearLayout)parent.findViewById(R.id.chart);
									//clear the previous layout:
									layout.removeAllViews();
									//add new graph:
									layout.addView(lineView);
								}
							});*/
			        	
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
			
	  public boolean initialize(Context context) {
		     if (mBluetoothManager == null) {
		            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
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


}
