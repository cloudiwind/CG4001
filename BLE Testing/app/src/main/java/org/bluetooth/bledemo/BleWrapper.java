package org.bluetooth.bledemo;

/******************************************************************************
BleWrapper is class that is part of the ECG Monitoring App done for a 
Wearable ECG Sensor Design FYP Project. These codes shall not be made public
or redistributed without permission.

This program is a wrapper class for BLE. It does the set up of the application
and handles BLE callbacks. 
More important comments are above each method in the code.

@author         Cloudi Ng
@email          cloudi.ng@u.nus.edu
@since          2017-Dec-20
@last modified  2018-Apr-11

*******************************************************************************/

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

public class BleWrapper {
	// defines (in milliseconds) how often RSSI should be updated
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

    // callback object through which we are returning results to the caller
    private BleWrapperUiCallbacks mUiCallback = null;
    // define NULL object for UI callbacks
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null(); 
    
    // creates BleWrapper object, set its parent activity and callback object
    public BleWrapper(Activity parent, BleWrapperUiCallbacks callback) {
    	this.mParent = parent;
    	mUiCallback = callback;
    	if(mUiCallback == null) mUiCallback = NULL_CALLBACK;
    }

    public BluetoothManager           getManager() { return mBluetoothManager; }
    public BluetoothAdapter           getAdapter() { return mBluetoothAdapter; }
    public BluetoothDevice            getDevice()  { return mBluetoothDevice; }
    public BluetoothGatt              getGatt()    { return mBluetoothGatt; }
    public BluetoothGattService       getCachedService() { return mBluetoothSelectedService; }
    public List<BluetoothGattService> getCachedServices() { return mBluetoothGattServices; }
    public boolean                    isConnected() { return mConnected; }

	// run test and check if this device has BT and BLE hardware available
	public boolean checkBleHardwareAvailable() {
		// check general Bluetooth Hardware:
		// get BluetoothManager
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) return false;
		// .. and then get adapter from manager
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) return false;
		
		// check if BLE is also available
		boolean hasBle = mParent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		return hasBle;
	}    


	// check if BT enabled. call this in onResume to make sure to check BT ON, then proceed with further action
	public boolean isBtEnabled() {
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) return false;
		
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) return false;
		
		return adapter.isEnabled();
	}
	
	// scan for BLE devices around
	public void startScanning() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        mBluetoothAdapter.getBluetoothLeScanner().startScan(null, builder.build(), mDeviceFoundCallback);
	}
	
	// stop scanning after timeout
	public void stopScanning() {
		mBluetoothAdapter.getBluetoothLeScanner().stopScan(mDeviceFoundCallback);
	}

    // initialise BLE
    // get BT manager and adapter ready for action
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        if(mBluetoothAdapter == null) mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;    	
    }

    // connect to device that has a specific address
    public boolean connect(final String deviceAddress) {
        if (mBluetoothAdapter == null || deviceAddress == null) return false;
        mDeviceAddress = deviceAddress;
        
        // check if we need to connect from scratch or just reconnect to previous device
        if(mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(deviceAddress)) {
        	// just reconnect
        	return mBluetoothGatt.connect();
        }
        else {
        	// connect from scratch
            // get BluetoothDevice object for specified address
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            if (mBluetoothDevice == null) {
                // we got wrong address - that device is not available!
                return false;
            }
            // connect with remote device
        	mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false, mBleCallback);
        }
        return true;
    }  

    // disconnect device.
    // suppose to reconnect to device later with GATT client, but sometimes don't work
    // to be worked on
    public void disconnect() {
    	if(mBluetoothGatt != null) mBluetoothGatt.disconnect();
    	 mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
    }

    // close GATT client completely
    public void close() {
    	if(mBluetoothGatt != null) mBluetoothGatt.close();
    	mBluetoothGatt = null;
    }    

    // prompt to read new RSSI value in accordance to the #ms set above
    public void readPeriodicalyRssiValue(final boolean repeat) {
    	mTimerEnabled = repeat;
    	// check if we should stop checking RSSI value
    	if(mConnected == false || mBluetoothGatt == null || mTimerEnabled == false) {
    		mTimerEnabled = false;
    		return;
    	}
    	
    	mTimerHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(mBluetoothGatt == null ||
				   mBluetoothAdapter == null ||
				   mConnected == false)
				{
					mTimerEnabled = false;
					return;
				}
				
				// request RSSI value
				mBluetoothGatt.readRemoteRssi();
				// add call it once more
				readPeriodicalyRssiValue(mTimerEnabled);
			}
    	}, RSSI_UPDATE_TIME_INTERVAL);
    }    
    
    // monitor RSSI value
    public void startMonitoringRssiValue() {
    	readPeriodicalyRssiValue(true);
    }
    
    // stop monitor RSSI value
    public void stopMonitoringRssiValue() {
    	readPeriodicalyRssiValue(false);
    }

    // call to discover all available services on the device
    // results returned to by calling the callback object
    public void startServicesDiscovery() {
    	if(mBluetoothGatt != null) mBluetoothGatt.discoverServices();
    }

    // get the appropriate services the device handles, then use the UI callback to deal with this shit
    // **PLS RMB TO CALL SERVICE DISCOVERY AND MAKE SURE IT IS FINISHED BEFORE CALLING GETSERVICES()
    public void getSupportedServices() {
    	if(mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) mBluetoothGattServices.clear();
    	// reference to services in local array
        if(mBluetoothGatt != null) mBluetoothGattServices = mBluetoothGatt.getServices();
        
        mUiCallback.uiAvailableServices(mBluetoothGatt, mBluetoothDevice, mBluetoothGattServices);
    }

    // draw out characteristics for a particular service and throws it back to the UI callback
    public void getCharacteristicsForService(final BluetoothGattService service) {
    	if(service == null) return;
    	List<BluetoothGattCharacteristic> chars = null;
    	
    	chars = service.getCharacteristics();   	
    	mUiCallback.uiCharacteristicForService(mBluetoothGatt, mBluetoothDevice, service, chars);
    	// keep reference to the last selected service
    	mBluetoothSelectedService = service;
    }

    // request to fetch newest value stored on the remote device for particular characteristic
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;
        
        mBluetoothGatt.readCharacteristic(ch);
        // new value available will be notified in Callback Object
    }


    // for some types of characteristics, we need to get the characteristic's value.
    // after that, call this mtd
    // MUST RMB TO UPDATE THE VALUE BY CALLING REQUESTCHARACTERISTICVALUE() FIRST
    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;
        
        byte[] rawValue = ch.getValue();
        String strValue = null;
        int intValue = 0;

        // read and parse characteristic to read value
        UUID uuid = ch.getUuid();
        
        if(uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT)) { // heart rate by default as ref frm def code
        	// follow https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        	// first check format used by the device - it is specified in bit 0 and tells us if we should ask for index 1 (and uint8) or index 2 (and uint16)
        	int index = ((rawValue[0] & 0x01) == 1) ? 2 : 1;
        	// define format
        	int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;
        	// get the value
        	intValue = ch.getIntValue(format, index);
        	strValue = intValue + " bpm";


        }
        else if (uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT) || // manufacturer name string
        		 uuid.equals(BleDefinedUUIDs.Characteristic.MODEL_NUMBER_STRING) || // model number string)
        		 uuid.equals(BleDefinedUUIDs.Characteristic.FIRMWARE_REVISION_STRING)) // firmware revision string
        {
        	// follow https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.manufacturer_name_string.xml etc.
        	// string value are usually simple utf8s string at index 0
        	strValue = ch.getStringValue(0);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.APPEARANCE)) { // appearance
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.gap.appearance.xml
        	intValue  = ((int)rawValue[1]) << 8;
        	intValue += rawValue[0];
        	strValue = BleNamesResolver.resolveAppearance(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BODY_SENSOR_LOCATION)) { // body sensor location
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.body_sensor_location.xml
        	intValue = rawValue[0];
        	strValue = BleNamesResolver.resolveHeartRateSensorLocation(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BATTERY_LEVEL)) { // battery level
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.battery_level.xml
        	intValue = rawValue[0];
        	strValue = "" + intValue + "% battery level";
        }        
        else {
        	// not known type of characteristic, so we need to handle this in "general" way
        	// get first four bytes and transform it to integer
        	intValue = 0;
        	if(rawValue.length > 0) intValue = (int)rawValue[0];
        	if(rawValue.length > 1) intValue = intValue + ((int)rawValue[1] << 8); 
        	if(rawValue.length > 2) intValue = intValue + ((int)rawValue[2] << 8); 
        	if(rawValue.length > 3) intValue = intValue + ((int)rawValue[3] << 8); 

        	// try catch error.
            // invalid unicode point after value of 127.
            // stupid reason why unable to print MSB =1 values
            
        	try {
            if (rawValue.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(rawValue.length);
                for(byte byteChar : rawValue) {
                    if (((byteChar & 0x08) >> 7) == 0) {
                        stringBuilder.append(String.format("%c", byteChar));
                    }
                }
                strValue = stringBuilder.toString();
            }} catch (Exception e) {
        	    strValue = "-"; //to prevent null exception
            }
        }
        
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(new Date());
        mUiCallback.uiNewValueForCharacteristic(mBluetoothGatt,
                                                mBluetoothDevice,
                                                mBluetoothSelectedService,
        		                                ch,
        		                                strValue,
        		                                intValue,
        		                                rawValue,
        		                                timestamp);
    }    

    // return the format as indicated by characteristic's properties
    // for shitz some use some dont use wtf?
    public int getValueFormat(BluetoothGattCharacteristic ch) {
    	int properties = ch.getProperties();
    	
    	if((BluetoothGattCharacteristic.FORMAT_FLOAT & properties) != 0) return BluetoothGattCharacteristic.FORMAT_FLOAT;
    	if((BluetoothGattCharacteristic.FORMAT_SFLOAT & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SFLOAT;
    	if((BluetoothGattCharacteristic.FORMAT_SINT16 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT16;
    	if((BluetoothGattCharacteristic.FORMAT_SINT32 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT32;
    	if((BluetoothGattCharacteristic.FORMAT_SINT8 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT8;
    	if((BluetoothGattCharacteristic.FORMAT_UINT16 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT16;
    	if((BluetoothGattCharacteristic.FORMAT_UINT32 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT32;
    	if((BluetoothGattCharacteristic.FORMAT_UINT8 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT8;
    	
    	return 0;
    }

    // set new value for particular characteristic
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
    	if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;
    	
    	// set value locally
    	ch.setValue(dataToWrite);
    	// push the change and commit to peripheral
    	mBluetoothGatt.writeCharacteristic(ch);
    }

    // set if that characteristic should be using set notify
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;
        
        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if(!success) {
        	Log.e("------", "Setting proper notification status for characteristic failed!");
        } //use for debugging. mayb shud toast.

        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        // to enable notify and indications - esp impt for HR monitor
        // fkin impt shit here man, common mistake. besides enabling true for setnotif(), must have descriptor!
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if(descriptor != null) {
        	byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
	        descriptor.setValue(val);
	        mBluetoothGatt.writeDescriptor(descriptor);
        }
    }


    private ScanCallback mDeviceFoundCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            mUiCallback.uiDeviceFound(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
        }

        /**
         * Callback when batch results are delivered.
         *
         * @param results List of scan results that are previously scanned.
         */
        public void onBatchScanResults(List<ScanResult> results) {
        }

        /**
         * Callback when scan could not be started.
         *
         * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
         */
        public void onScanFailed(int errorCode) {
        }

    };

    // ble device use dis callbacks for any action
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
            	mConnected = true;
            	mUiCallback.uiDeviceConnected(mBluetoothGatt, mBluetoothDevice);

                // talk w device
            	mBluetoothGatt.readRemoteRssi();
            	// this is updated by throwing value to callback obj

                // auto call to discover service
            	startServicesDiscovery();
            	
            	// update RSSI periodically as stated above
            	startMonitoringRssiValue();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
//                try {
//                    mBluetoothGatt.close();
//                } catch (Exception e) {
//                    Log.d("DDDD", "close ignoring: " + e);
//                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	// services discovery is finished, we can call getServices() for Gatt
            	getSupportedServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
        	// fetch characteristic value after req
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	// and it success, so we can get the value
                Log.e("test", "read");
            	getCharacteristicValue(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
        	// characteristic's value was updated due to enabled notification, lets get this value
        	// the value itself will be reported to the UI inside getCharacteristicValue
        	getCharacteristicValue(characteristic);
        	// also, notify UI that notification are enabled for particular characteristic
            Log.e("test", "changed");
        	mUiCallback.uiGotNotification(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic);
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	String deviceName = gatt.getDevice().getName();
        	String serviceName = BleNamesResolver.resolveServiceName(characteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault()));
        	String charName = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault()));
        	String description = "Device: " + deviceName + " Service: " + serviceName + " Characteristic: " + charName;
        	
        	// we got response regarding our request to write new value to the characteristic
        	// let see if it failed or not
        	if(status == BluetoothGatt.GATT_SUCCESS) {
        		 mUiCallback.uiSuccessfulWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description);
        	}
        	else {
        		 mUiCallback.uiFailedWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description + " STATUS = " + status);
        	}
        };
        
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        	if(status == BluetoothGatt.GATT_SUCCESS) {
        		// we got new value of RSSI of the connection, pass it to the UI
        		 mUiCallback.uiNewRssiAvailable(mBluetoothGatt, mBluetoothDevice, rssi);
        	}
        };
    };
    
	private Activity mParent = null;    
	private boolean mConnected = false;
	private String mDeviceAddress = "";

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice  mBluetoothDevice = null;
    private BluetoothGatt    mBluetoothGatt = null;
    private BluetoothGattService mBluetoothSelectedService = null;
    private List<BluetoothGattService> mBluetoothGattServices = null;	
    
    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
}
