package com.example.make201512.bluetoothconnector;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by Tom on 2016/4/26.
 * 经典蓝牙连接辅助类
 */
public class BluetoothClassicConnector {

    BluetoothAdapter bluetoothAdapter;

    public void connect(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

}
