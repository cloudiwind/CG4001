package org.bluetooth.bledemo;

/******************************************************************************
CharacteristicDetailsAdapter is class that is part of the ECG Monitoring App 
done for a  Wearable ECG Sensor Design FYP Project. 
These codes shall not be made public or redistributed without permission.

This program deals with data received by the BLE and plots them into a graph.
It also deals with what is being viewed on the mobile device and when the
screen refreshes. More important comments are above each method in the code.

@author 		Cloudi Ng
@email 			cloudi.ng@u.nus.edu
@since 			2017-Dec-20
@last modified 	2018-Apr-11

*******************************************************************************/

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class CharacteristicDetailsAdapter extends BaseAdapter {
   	
	private BluetoothGattCharacteristic mCharacteristic = null;
	private LayoutInflater mInflater;
	private BleWrapper mBleWrapper = null;
	private byte[] mRawValue = null;
	private int mIntValue = 0;
	private String mAsciiValue = "";
	private String mStrValue = "";
	private String mLastUpdateTime = "";
	private boolean mNotificationEnabled = false;

	private LineGraphSeries<DataPoint> mSeries2 = new LineGraphSeries<>();
	private double graph2LastXValue = 0;
	private boolean newValueExist = false;
	
	public CharacteristicDetailsAdapter(PeripheralActivity parent, BleWrapper ble) {
		super();
		mBleWrapper = ble;
		mInflater = parent.getLayoutInflater();
	}
	
	public void setCharacteristic(BluetoothGattCharacteristic ch) {
		this.mCharacteristic = ch;
		mRawValue = null;
		mIntValue = 0;
		mAsciiValue = "";
		mStrValue = "";
		mLastUpdateTime = "-";
		mNotificationEnabled = false;
	}
	
	public BluetoothGattCharacteristic getCharacteristic(int index) {
		return mCharacteristic;
	}

	public void clearCharacteristic() {
		mCharacteristic = null;
	}
	
	@Override
	public int getCount() {
		return (mCharacteristic != null) ? 1 : 0;
	}

	@Override
	public Object getItem(int position) {
		return mCharacteristic;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void newValueForCharacteristic(final BluetoothGattCharacteristic ch, final String strVal, final int intVal, final byte[] rawValue, final String timestamp) {
		if(!ch.equals(this.mCharacteristic)) return;
		
		mIntValue = intVal;
		mStrValue = strVal;
		mRawValue = rawValue;
		Log.e("bytecheck3", "testing" + Arrays.toString(rawValue));
        if (mRawValue != null && mRawValue.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(mRawValue.length);
            for(byte byteChar : mRawValue)
                stringBuilder.append(String.format("%02X", byteChar));
            mAsciiValue = "0x" + stringBuilder.toString();
        }
        else mAsciiValue = "";
        
        mLastUpdateTime = timestamp;
        if(mLastUpdateTime == null) mLastUpdateTime = "";

        newValueExist = true;
	}
	
	public void setNotificationEnabledForService(final BluetoothGattCharacteristic ch) {
		Log.e("test", "notified_1");
		if((!ch.equals(this.mCharacteristic)) || (mNotificationEnabled == true)) return;
		//if((!ch.equals(this.mCharacteristic))) return;
		mNotificationEnabled = true;
		Log.e("test", "notified_2");
		notifyDataSetChanged();
	}
	
	public byte[] parseHexStringToBytes(final String hex) {
		String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
		byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally
		
		String part = "";
		
		for(int i = 0; i < bytes.length; ++i) {
			part = "0x" + tmp.substring(i*2, i*2+2);
			bytes[i] = Long.decode(part).byteValue();
		}
		
		return bytes;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup p) {
		// get already available view or create new if necessary
		FieldReferences fields;
        if (convertView == null) {
        	fields = new FieldReferences();
        	convertView = mInflater.inflate(R.layout.peripheral_details_characteristic_item,null);
        	fields.charPeripheralName = (TextView)convertView.findViewById(R.id.char_details_peripheral_name);
        	fields.charPeripheralAddress = (TextView)convertView.findViewById(R.id.char_details_peripheral_address);
        	fields.charServiceName = (TextView)convertView.findViewById(R.id.char_details_service);
        	fields.charServiceUuid = (TextView)convertView.findViewById(R.id.char_details_service_uuid);
        	fields.charName = (TextView)convertView.findViewById(R.id.char_details_name);
        	fields.charUuid = (TextView)convertView.findViewById(R.id.char_details_uuid);
        	
        	fields.charDataType = (TextView)convertView.findViewById(R.id.char_details_type);
        	fields.charProperties = (TextView) convertView.findViewById(R.id.char_details_properties);
        	
        	fields.charStrValue = (TextView) convertView.findViewById(R.id.char_details_ascii_value);
        	fields.charDecValue = (TextView) convertView.findViewById(R.id.char_details_decimal_value);
        	fields.charHexValue = (EditText) convertView.findViewById(R.id.char_details_hex_value);
        	fields.charDateValue = (TextView) convertView.findViewById(R.id.char_details_timestamp);
        	
        	fields.notificationBtn = (ToggleButton) convertView.findViewById(R.id.char_details_notification_switcher);
        	fields.readBtn = (Button) convertView.findViewById(R.id.char_details_read_btn);
        	fields.writeBtn = (Button) convertView.findViewById(R.id.char_details_write_btn);
        	fields.writeBtn.setTag(fields.charHexValue);

			//empty graph

        	fields.lineGraph = (GraphView) convertView.findViewById(R.id.graph);
			fields.lineGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
			fields.lineGraph.addSeries(mSeries2);

			String uuid = mCharacteristic.getUuid().toString().toLowerCase(Locale.getDefault());
			String name = BleNamesResolver.resolveCharacteristicName(uuid);

			//setting range of print in x and y axis
			if (name.equals("ECG Wave") ) {
				fields.lineGraph.getViewport().setXAxisBoundsManual(true);
				//fields.lineGraph.getViewport().scrollToEnd();
				fields.lineGraph.getViewport().setMinX(0);
				fields.lineGraph.getViewport().setMaxX(600); //init 100
				fields.lineGraph.getViewport().setYAxisBoundsManual(true);
				fields.lineGraph.getViewport().setMinY(-6000);
				fields.lineGraph.getViewport().setMaxY(20000);
				//fields.lineGraph.getViewport().setScrollable(false); // enables horizontal scrolling
			}

			else if (name.equals("Heart Rate Measurement")){
				fields.lineGraph.getViewport().setXAxisBoundsManual(true);
				fields.lineGraph.getViewport().setMinX(0);
				fields.lineGraph.getViewport().setMaxX(40);
				fields.lineGraph.getViewport().setYAxisBoundsManual(true);
				fields.lineGraph.getViewport().setMinY(0);
				fields.lineGraph.getViewport().setMaxY(300);
				//fields.lineGraph.getViewport().setScrollable(true); // enables horizontal scrolling
				//fields.lineGraph.getViewport().setScrollableY(true); // enables vertical scrolling
				//fields.lineGraph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
				//fields.lineGraph.getViewport().setScalableY(true); // enables vertical zooming and scrolling
			}


        	fields.readBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mBleWrapper.requestCharacteristicValue(mCharacteristic);
				}
			});

        	fields.writeBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText hex = (EditText) v.getTag();
					String newValue =  hex.getText().toString().toLowerCase(Locale.getDefault());
					byte[] dataToWrite = parseHexStringToBytes(newValue);

					mBleWrapper.writeDataToCharacteristic(mCharacteristic, dataToWrite);
				}
			});          	
        	
        	fields.notificationBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked == mNotificationEnabled) return; // no need to update anything

					mBleWrapper.setNotificationForCharacteristic(mCharacteristic, isChecked);
					mNotificationEnabled = isChecked;
				}
			} );
        	
            convertView.setTag(fields);
        } else {
            fields = (FieldReferences) convertView.getTag();
        }			
		
        // set proper values into the view
        fields.charPeripheralName.setText(mBleWrapper.getDevice().getName());
        fields.charPeripheralAddress.setText(mBleWrapper.getDevice().getAddress());
        
        String tmp = mCharacteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault());
        fields.charServiceUuid.setText(tmp);
        fields.charServiceName.setText(BleNamesResolver.resolveServiceName(tmp));
        
        String uuid = mCharacteristic.getUuid().toString().toLowerCase(Locale.getDefault());
        String name = BleNamesResolver.resolveCharacteristicName(uuid);
        
        fields.charName.setText(name);
        fields.charUuid.setText(uuid);
        
        int format = mBleWrapper.getValueFormat(mCharacteristic);
        fields.charDataType.setText(BleNamesResolver.resolveValueTypeDescription(format));
        int props = mCharacteristic.getProperties();
        String propertiesString = String.format("0x%04X [", props);
        if((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0) propertiesString += "read ";
        if((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) propertiesString += "write ";
        if((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) propertiesString += "notify ";
        if((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) propertiesString += "indicate ";
        if((props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) propertiesString += "write_no_response ";
        fields.charProperties.setText(propertiesString + "]");
        
        fields.notificationBtn.setEnabled((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
        fields.notificationBtn.setChecked(mNotificationEnabled);
        fields.readBtn.setEnabled((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
        fields.writeBtn.setEnabled((props & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0);
        fields.charHexValue.setEnabled(fields.writeBtn.isEnabled());
        
        fields.charHexValue.setText(mAsciiValue);
        fields.charStrValue.setText(mStrValue);
        //Value sent
        fields.charDecValue.setText(String.format("%d", mIntValue));
        fields.charDateValue.setText(mLastUpdateTime);
		if(newValueExist) {
			populateGraph(mRawValue, name);
			newValueExist = false;
		}
   
        return convertView;
	}

	//Draw Graph here. Refer to documentation. http://www.android-graphview.org/realtime-chart/

	private void populateGraph(byte[] byteValues, String name){
		//From 40 to 100
		if (name.equals("ECG Wave")){
			for (int i =0; i<20; i=i+2) {
				mSeries2.appendData(new DataPoint(graph2LastXValue, generatePoints(byteValues, name, i)), true, 600);
				graph2LastXValue += 1d;
				Log.e("bytecheck", "testing graph2LastXValue: " + graph2LastXValue + "i= " + i);
			}

		}

		else if (name.equals("Heart Rate Measurement")){
			mSeries2.appendData(new DataPoint(graph2LastXValue, mIntValue), true, 40);
			graph2LastXValue += 1d;
		}

	}

	//Compute Points
	//mIntValue, light shift << and OR
	private int generatePoints(byte[] byteValues, String name, int i){
		//double mLastRandom = 2;
		//Random mRand = new Random();
		//mLastRandom += mRand.nextDouble() * 0.5 - 0.25;
		//mIntValue += mRand.nextDouble() * 0.5 - 0.25;

		//return (int) mLastRandom;

		short intVal = 0;
		if(byteValues != null && name.equals("ECG Wave")) {
			//Log.e("bytecheck", "testing" + Arrays.toString(byteValues) + " " + intVal);
			/* for testing
			intVal = (short)(byteValues[1] | ((byteValues[0] & 0xf) << 4) | ((byteValues[3] & 0xf) << 8) | ((byteValues[2] & 0xf) << 12)); */
			/* used this
			intVal = (short)(byteValues[3] | ((byteValues[2] & 0xf) << 4) | ((byteValues[1] & 0xf) << 8) | ((byteValues[0] & 0xf) << 12)); */

			// masking to prevent possible extension of bits
			intVal = (short)((byteValues[i+1] & 0xff) | ((byteValues[i] & 0xff) << 8));
			//intVal = (short)((byteValues[1]) | (byteValues[0] << 8));
//1 0 3 2
			//3 2 1 0
		}

		//byte byte1 = (byte)(intVal >> 8 & 0xff);
		//byte byte2 = (byte)(intVal & 0xff);
		Log.e("bytecheck2", "testing" + intVal);

		//implement butterworth / FIR filter here.

		return (int) intVal;
	}
    	
	private class FieldReferences {
		TextView charPeripheralName;
		TextView charPeripheralAddress;
		TextView charServiceName;
		TextView charServiceUuid;
		TextView charUuid;
		TextView charName;
		TextView charDataType;
		TextView charStrValue;
		EditText charHexValue;
		TextView charDecValue;
		TextView charDateValue;
		TextView charProperties;

		GraphView lineGraph;
		
		ToggleButton notificationBtn;
		Button readBtn;
		Button writeBtn;
	}
}
