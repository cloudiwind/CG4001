package org.bluetooth.bledemo;

/******************************************************************************
CharacteristicListAdapter is class that is part of the ECG Monitoring App 
done for a  Wearable ECG Sensor Design FYP Project. 
These codes shall not be made public or redistributed without permission.

This program refreshes the proper values into the getView. And puts all 
available Characteristics for that service into a list.
More important comments are above each method in the code.

@author         Cloudi Ng
@email          cloudi.ng@u.nus.edu
@since          2017-Dec-20
@last modified  2018-Apr-11

*******************************************************************************/

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CharacteristicsListAdapter extends BaseAdapter {
    	private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    	private LayoutInflater mInflater;
    	
    	public CharacteristicsListAdapter(Activity parent) {
    		super();
    		mCharacteristics  = new ArrayList<BluetoothGattCharacteristic>();
    		mInflater = parent.getLayoutInflater();
    	}
    	
    	public void addCharacteristic(BluetoothGattCharacteristic ch) {
    		if(mCharacteristics.contains(ch) == false) {
    			mCharacteristics.add(ch);
    		}
    	}
    	
    	public BluetoothGattCharacteristic getCharacteristic(int index) {
    		return mCharacteristics.get(index);
    	}

    	public void clearList() {
    		mCharacteristics.clear();
    	}
    	
		@Override
		public int getCount() {
			return mCharacteristics.size();
		}

		@Override
		public Object getItem(int position) {
			return getCharacteristic(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// get already available view or create new if necessary
			FieldReferences fields;
            if (convertView == null) {
            	convertView = mInflater.inflate(R.layout.peripheral_list_characteristic_item, null);
            	fields = new FieldReferences();
            	fields.charName = (TextView)convertView.findViewById(R.id.peripheral_list_characteristic_name);
            	fields.charUuid = (TextView)convertView.findViewById(R.id.peripheral_list_characteristic_uuid);
                convertView.setTag(fields);
            } else {
                fields = (FieldReferences) convertView.getTag();
            }			
			
            // set proper values into the view
            BluetoothGattCharacteristic ch = getCharacteristic(position);
            String uuid = ch.getUuid().toString().toLowerCase(Locale.getDefault());
            String name = BleNamesResolver.resolveCharacteristicName(uuid);
            
            fields.charName.setText(name);
            fields.charUuid.setText(uuid);
   
			return convertView;
		}
    	
		private class FieldReferences {
			TextView charName;
			TextView charUuid;
		}
}
