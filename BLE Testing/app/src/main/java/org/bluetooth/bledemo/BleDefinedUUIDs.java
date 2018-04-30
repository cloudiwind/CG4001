package org.bluetooth.bledemo;

/******************************************************************************
BleDefinedUUIDs is class that is part of the ECG Monitoring App done for a 
Wearable ECG Sensor Design FYP Project. These codes shall not be made public
or redistributed without permission.

This program defines standard UUIDs.
More important comments are above each method in the code.

@author 		Cloudi Ng
@email 			cloudi.ng@u.nus.edu
@since 			2017-Dec-20
@last modified 	2018-Apr-11

*******************************************************************************/

import java.util.UUID;

public class BleDefinedUUIDs {
	
	public static class Service {
		final static public UUID HEART_RATE               = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
	};
	
	public static class Characteristic {
		final static public UUID HEART_RATE_MEASUREMENT   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
		final static public UUID MANUFACTURER_STRING      = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
		final static public UUID MODEL_NUMBER_STRING      = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
		final static public UUID FIRMWARE_REVISION_STRING = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
		final static public UUID APPEARANCE               = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
		final static public UUID BODY_SENSOR_LOCATION     = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
		final static public UUID BATTERY_LEVEL            = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
		//final static public UUID ECG_TEST				  = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	}
	
	public static class Descriptor {
		final static public UUID CHAR_CLIENT_CONFIG       = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	}
	
}
